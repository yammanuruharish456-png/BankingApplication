package com.banking.upi.payment.transfer;

import com.banking.upi.domain.*;
import com.banking.upi.domain.Transaction.TxnStatus;
import com.banking.upi.domain.Transaction.TxnType;
import com.banking.upi.exception.ApiException;
import com.banking.upi.payment.transfer.dto.TransferRequest;
import com.banking.upi.payment.transfer.dto.TransferResponse;
import com.banking.upi.pin.PinService;
import com.banking.upi.repository.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class TransferService {

    private final VpaRepository vpaRepository;
    private final BalanceRepository balanceRepository;
    private final TransactionRepository transactionRepository;
    private final LedgerEntryRepository ledgerEntryRepository;
    private final OutboxEventRepository outboxEventRepository;
    private final IdempotencyKeyRepository idempotencyKeyRepository;
    private final AuditLogRepository auditLogRepository;
    private final UserRepository userRepository;
    private final PinService pinService;
    private final ObjectMapper objectMapper;

    @Transactional
    public TransferResponse executeTransfer(String idempotencyKeyValue, TransferRequest request) {
        // Step 1: Check idempotency
        var existingKey = idempotencyKeyRepository.findByIdempotencyKey(idempotencyKeyValue);
        if (existingKey.isPresent()) {
            try {
                return objectMapper.readValue(existingKey.get().getResponseBody(), TransferResponse.class);
            } catch (Exception e) {
                log.error("Could not deserialize cached response for idempotency key: {}. Rejecting to prevent duplicate transaction.", idempotencyKeyValue);
                throw new ApiException(HttpStatus.CONFLICT, "Duplicate idempotency key with unreadable cached response");
            }
        }

        // Step 2: Resolve payer VPA
        Vpa payerVpa = vpaRepository.findByHandle(request.getPayerVpa())
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Payer VPA not found: " + request.getPayerVpa()));
        if (!payerVpa.getActive()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Payer VPA is inactive");
        }

        // Resolve payee VPA
        Vpa payeeVpa = vpaRepository.findByHandle(request.getPayeeVpa())
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Payee VPA not found: " + request.getPayeeVpa()));
        if (!payeeVpa.getActive()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Payee VPA is inactive");
        }

        if (payerVpa.getAccountId().equals(payeeVpa.getAccountId())) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Payer and payee cannot be the same account");
        }

        // Step 3: Verify UPI PIN
        pinService.verifyPin(payerVpa.getUserId(), request.getUpiPin());

        // Step 4 & 5: Check balance with SELECT FOR UPDATE
        Balance payerBalance = balanceRepository.findByAccountIdWithLock(payerVpa.getAccountId())
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Payer balance not found"));

        if (payerBalance.getAmount().compareTo(request.getAmount()) < 0) {
            throw new ApiException(HttpStatus.PAYMENT_REQUIRED, "Insufficient balance");
        }

        Balance payeeBalance = balanceRepository.findByAccountIdWithLock(payeeVpa.getAccountId())
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Payee balance not found"));

        // Step 6: Create Transaction row (PENDING)
        String txnRef = "TXN" + UUID.randomUUID().toString().replace("-", "").substring(0, 16).toUpperCase();
        Transaction transaction = Transaction.builder()
                .txnRef(txnRef)
                .payerVpa(request.getPayerVpa())
                .payeeVpa(request.getPayeeVpa())
                .payerAccountId(payerVpa.getAccountId())
                .payeeAccountId(payeeVpa.getAccountId())
                .amount(request.getAmount())
                .note(request.getNote())
                .status(TxnStatus.PENDING)
                .txnType(TxnType.PAY)
                .clientRef(request.getClientRef())
                .idempotencyKey(idempotencyKeyValue)
                .build();

        transaction = transactionRepository.save(transaction);

        // Step 7: Insert two LedgerEntry rows
        LedgerEntry debitEntry = LedgerEntry.builder()
                .txnId(transaction.getId())
                .accountId(payerVpa.getAccountId())
                .entryType(LedgerEntry.EntryType.DEBIT)
                .amount(request.getAmount())
                .build();

        LedgerEntry creditEntry = LedgerEntry.builder()
                .txnId(transaction.getId())
                .accountId(payeeVpa.getAccountId())
                .entryType(LedgerEntry.EntryType.CREDIT)
                .amount(request.getAmount())
                .build();

        ledgerEntryRepository.save(debitEntry);
        ledgerEntryRepository.save(creditEntry);

        // Step 8: Update both Balance rows
        payerBalance.setAmount(payerBalance.getAmount().subtract(request.getAmount()));
        payeeBalance.setAmount(payeeBalance.getAmount().add(request.getAmount()));
        balanceRepository.save(payerBalance);
        balanceRepository.save(payeeBalance);

        // Step 9: Insert OutboxEvent
        try {
            String eventPayload = objectMapper.writeValueAsString(
                    java.util.Map.of(
                            "txnId", transaction.getId(),
                            "txnRef", txnRef,
                            "payerVpa", request.getPayerVpa(),
                            "payeeVpa", request.getPayeeVpa(),
                            "amount", request.getAmount()
                    )
            );
            OutboxEvent outboxEvent = OutboxEvent.builder()
                    .aggregateType("Transaction")
                    .aggregateId(transaction.getId().toString())
                    .eventType("TRANSFER_SUCCESS")
                    .payload(eventPayload)
                    .published(false)
                    .build();
            outboxEventRepository.save(outboxEvent);
        } catch (Exception e) {
            log.warn("Failed to create outbox event for txnId={}", transaction.getId());
        }

        // Step 10: Update Transaction to SUCCESS
        transaction.setStatus(TxnStatus.SUCCESS);
        transaction = transactionRepository.save(transaction);

        // Build response
        TransferResponse response = TransferResponse.builder()
                .txnId(transaction.getId())
                .txnRef(transaction.getTxnRef())
                .status(transaction.getStatus())
                .amount(transaction.getAmount())
                .payerVpa(transaction.getPayerVpa())
                .payeeVpa(transaction.getPayeeVpa())
                .note(transaction.getNote())
                .createdAt(transaction.getCreatedAt())
                .build();

        // Step 11: Save IdempotencyKey
        try {
            String responseBody = objectMapper.writeValueAsString(response);
            IdempotencyKey iKey = IdempotencyKey.builder()
                    .idempotencyKey(idempotencyKeyValue)
                    .responseStatus(200)
                    .responseBody(responseBody)
                    .build();
            idempotencyKeyRepository.save(iKey);
        } catch (Exception e) {
            log.warn("Failed to save idempotency key for txnId={}", transaction.getId());
        }

        // Step 12: Save AuditLog
        AuditLog auditLog = AuditLog.builder()
                .userId(payerVpa.getUserId())
                .action("TRANSFER")
                .entityType("Transaction")
                .entityId(transaction.getId().toString())
                .details("Transfer of " + request.getAmount() + " from " + request.getPayerVpa() + " to " + request.getPayeeVpa())
                .build();
        auditLogRepository.save(auditLog);

        log.info("Transfer success: txnRef={}, amount={}, from={}, to={}",
                txnRef, request.getAmount(), request.getPayerVpa(), request.getPayeeVpa());

        return response;
    }

    public TransferResponse getTransaction(Long txnId) {
        Transaction txn = transactionRepository.findById(txnId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Transaction not found: " + txnId));

        return TransferResponse.builder()
                .txnId(txn.getId())
                .txnRef(txn.getTxnRef())
                .status(txn.getStatus())
                .amount(txn.getAmount())
                .payerVpa(txn.getPayerVpa())
                .payeeVpa(txn.getPayeeVpa())
                .note(txn.getNote())
                .createdAt(txn.getCreatedAt())
                .build();
    }
}
