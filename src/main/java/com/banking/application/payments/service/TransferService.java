package com.banking.application.payments.service;

import com.banking.application.common.exception.ApiException;
import com.banking.application.payments.dto.TransferRequest;
import com.banking.application.payments.dto.TransferResponse;
import com.banking.application.payments.entity.*;
import com.banking.application.payments.repository.*;
import com.banking.application.users.entity.User;
import com.banking.application.users.entity.VpaHandle;
import com.banking.application.users.repository.VpaHandleRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.Comparator;
import java.util.HexFormat;
import java.util.List;

@Service
public class TransferService {
    private static final String ENDPOINT_TRANSFER = "POST:/v1/payments/transfer";

    private final VpaHandleRepository vpaHandleRepository;
    private final BalanceRepository balanceRepository;
    private final TransactionRepository transactionRepository;
    private final LedgerEntryRepository ledgerEntryRepository;
    private final IdempotencyKeyRepository idempotencyKeyRepository;
    private final OutboxEventRepository outboxEventRepository;
    private final UpiPinService upiPinService;
    private final ObjectMapper objectMapper;

    public TransferService(VpaHandleRepository vpaHandleRepository,
                           BalanceRepository balanceRepository,
                           TransactionRepository transactionRepository,
                           LedgerEntryRepository ledgerEntryRepository,
                           IdempotencyKeyRepository idempotencyKeyRepository,
                           OutboxEventRepository outboxEventRepository,
                           UpiPinService upiPinService,
                           ObjectMapper objectMapper) {
        this.vpaHandleRepository = vpaHandleRepository;
        this.balanceRepository = balanceRepository;
        this.transactionRepository = transactionRepository;
        this.ledgerEntryRepository = ledgerEntryRepository;
        this.idempotencyKeyRepository = idempotencyKeyRepository;
        this.outboxEventRepository = outboxEventRepository;
        this.upiPinService = upiPinService;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public TransferResponse transfer(User caller, String idempotencyKeyHeader, TransferRequest request) {
        return transfer(caller, idempotencyKeyHeader, request, ENDPOINT_TRANSFER);
    }

    @Transactional
    public TransferResponse transfer(User caller, String idempotencyKeyHeader, TransferRequest request, String endpoint) {
        if (!request.payerVpa().equalsIgnoreCase(resolvePrimaryVpa(caller.getId()))) {
            throw new ApiException(HttpStatus.FORBIDDEN, "Payer VPA must belong to authenticated user");
        }

        String requestHash = hashRequest(request);
        IdempotencyKey idempotencyKey = idempotencyKeyRepository.findForUpdate(caller.getId(), idempotencyKeyHeader, endpoint).orElse(null);

        if (idempotencyKey != null) {
            if (!idempotencyKey.getRequestHash().equals(requestHash)) {
                throw new ApiException(HttpStatus.CONFLICT, "Idempotency key reused with different request");
            }
            if (idempotencyKey.getStatus() == IdempotencyStatus.COMPLETED && idempotencyKey.getResponsePayload() != null) {
                try {
                    return objectMapper.readValue(idempotencyKey.getResponsePayload(), TransferResponse.class);
                } catch (JsonProcessingException e) {
                    throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to deserialize idempotent response");
                }
            }
            if (idempotencyKey.getStatus() == IdempotencyStatus.PROCESSING) {
                throw new ApiException(HttpStatus.CONFLICT, "Request with this idempotency key is processing");
            }
        } else {
            idempotencyKey = new IdempotencyKey();
            idempotencyKey.setUser(caller);
            idempotencyKey.setKey(idempotencyKeyHeader);
            idempotencyKey.setEndpoint(endpoint);
            idempotencyKey.setRequestHash(requestHash);
            idempotencyKey.setStatus(IdempotencyStatus.PROCESSING);
            try {
                idempotencyKeyRepository.saveAndFlush(idempotencyKey);
            } catch (DataIntegrityViolationException e) {
                idempotencyKey = idempotencyKeyRepository.findForUpdate(caller.getId(), idempotencyKeyHeader, endpoint)
                        .orElseThrow(() -> new ApiException(HttpStatus.CONFLICT, "Duplicate idempotency key"));
            }
        }

        VpaHandle payerHandle = vpaHandleRepository.findByVpa(request.payerVpa())
                .orElseThrow(() -> new ApiException(HttpStatus.BAD_REQUEST, "Invalid payer VPA"));
        VpaHandle payeeHandle = vpaHandleRepository.findByVpa(request.payeeVpa())
                .orElseThrow(() -> new ApiException(HttpStatus.BAD_REQUEST, "Invalid payee VPA"));

        upiPinService.verify(payerHandle.getUser(), request.upiPin());

        List<User> orderedUsers = List.of(payerHandle.getUser(), payeeHandle.getUser()).stream()
                .distinct()
                .sorted(Comparator.comparing(User::getId))
                .toList();

        for (User user : orderedUsers) {
            ensureBalanceRow(user);
        }

        Balance payerBalance = balanceRepository.findByAccountIdForUpdate(payerHandle.getUser().getId())
                .orElseThrow(() -> new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "Missing payer balance"));
        Balance payeeBalance = balanceRepository.findByAccountIdForUpdate(payeeHandle.getUser().getId())
                .orElseThrow(() -> new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "Missing payee balance"));

        if (payerBalance.getAmount().compareTo(request.amount()) < 0) {
            idempotencyKey.setStatus(IdempotencyStatus.FAILED);
            idempotencyKeyRepository.save(idempotencyKey);
            throw new ApiException(HttpStatus.BAD_REQUEST, "Insufficient balance");
        }

        Transaction txn = new Transaction();
        txn.setClientRef(request.clientRef());
        txn.setPayerVpa(request.payerVpa());
        txn.setPayeeVpa(request.payeeVpa());
        txn.setAmount(request.amount());
        txn.setNote(request.note());
        txn.setStatus(TransactionStatus.PENDING);
        txn = transactionRepository.save(txn);

        LedgerEntry debit = new LedgerEntry();
        debit.setTransaction(txn);
        debit.setAccount(payerHandle.getUser());
        debit.setEntryType(LedgerEntryType.DEBIT);
        debit.setAmount(request.amount());
        ledgerEntryRepository.save(debit);

        LedgerEntry credit = new LedgerEntry();
        credit.setTransaction(txn);
        credit.setAccount(payeeHandle.getUser());
        credit.setEntryType(LedgerEntryType.CREDIT);
        credit.setAmount(request.amount());
        ledgerEntryRepository.save(credit);

        payerBalance.setAmount(payerBalance.getAmount().subtract(request.amount()));
        payeeBalance.setAmount(payeeBalance.getAmount().add(request.amount()));
        balanceRepository.save(payerBalance);
        balanceRepository.save(payeeBalance);

        OutboxEvent outbox = new OutboxEvent();
        outbox.setAggregateType("TRANSACTION");
        outbox.setAggregateId(String.valueOf(txn.getId()));
        outbox.setEventType("PAYMENT_SUCCESS");
        outbox.setPayloadJson(writeJson(new TransferResponse(txn.getId(), TransactionStatus.SUCCESS.name())));
        outbox.setStatus(OutboxStatus.PENDING);
        outboxEventRepository.save(outbox);

        txn.setStatus(TransactionStatus.SUCCESS);
        transactionRepository.save(txn);

        TransferResponse response = new TransferResponse(txn.getId(), txn.getStatus().name());
        idempotencyKey.setStatus(IdempotencyStatus.COMPLETED);
        idempotencyKey.setResponsePayload(writeJson(response));
        idempotencyKeyRepository.save(idempotencyKey);
        return response;
    }

    private String writeJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException e) {
            throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "Serialization failed");
        }
    }

    private String resolvePrimaryVpa(Long userId) {
        return vpaHandleRepository.findFirstByUserId(userId)
                .map(VpaHandle::getVpa)
                .orElseThrow(() -> new ApiException(HttpStatus.BAD_REQUEST, "User has no VPA"));
    }

    private void ensureBalanceRow(User user) {
        if (balanceRepository.findByAccountIdForUpdate(user.getId()).isPresent()) {
            return;
        }
        Balance balance = new Balance();
        balance.setAccount(user);
        balance.setAmount(BigDecimal.ZERO);
        try {
            balanceRepository.saveAndFlush(balance);
        } catch (DataIntegrityViolationException ignored) {
        }
    }

    private String hashRequest(TransferRequest request) {
        String canonical = request.payerVpa() + "|" + request.payeeVpa() + "|" + request.amount().toPlainString() + "|"
                + (request.note() == null ? "" : request.note()) + "|" + request.clientRef();
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(canonical.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) {
            throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to hash request");
        }
    }
}
