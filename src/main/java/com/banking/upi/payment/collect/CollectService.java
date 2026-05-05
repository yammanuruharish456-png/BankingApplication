package com.banking.upi.payment.collect;

import com.banking.upi.domain.CollectRequest;
import com.banking.upi.domain.CollectRequest.CollectStatus;
import com.banking.upi.exception.ApiException;
import com.banking.upi.payment.collect.dto.ApproveRequest;
import com.banking.upi.payment.collect.dto.CollectRequestDto;
import com.banking.upi.payment.collect.dto.CollectResponse;
import com.banking.upi.payment.transfer.TransferService;
import com.banking.upi.payment.transfer.dto.TransferRequest;
import com.banking.upi.repository.CollectRequestRepository;
import com.banking.upi.repository.VpaRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class CollectService {

    private final CollectRequestRepository collectRequestRepository;
    private final VpaRepository vpaRepository;
    private final TransferService transferService;

    @Transactional
    public CollectResponse createCollectRequest(CollectRequestDto dto) {
        vpaRepository.findByHandle(dto.getFromVpa())
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "From VPA not found: " + dto.getFromVpa()));
        vpaRepository.findByHandle(dto.getToVpa())
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "To VPA not found: " + dto.getToVpa()));

        String requestRef = "COLL" + UUID.randomUUID().toString().replace("-", "").substring(0, 16).toUpperCase();

        CollectRequest collectRequest = CollectRequest.builder()
                .requestRef(requestRef)
                .fromVpa(dto.getFromVpa())
                .toVpa(dto.getToVpa())
                .amount(dto.getAmount())
                .note(dto.getNote())
                .status(CollectStatus.PENDING)
                .expiresAt(LocalDateTime.now().plusHours(24))
                .build();

        collectRequest = collectRequestRepository.save(collectRequest);
        log.info("Collect request created: ref={}", requestRef);

        return mapToResponse(collectRequest);
    }

    @Transactional
    public CollectResponse approveCollectRequest(Long requestId, String idempotencyKey, ApproveRequest approveRequest) {
        CollectRequest collectRequest = collectRequestRepository.findById(requestId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Collect request not found"));

        if (collectRequest.getStatus() != CollectStatus.PENDING) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Collect request is not pending");
        }

        if (LocalDateTime.now().isAfter(collectRequest.getExpiresAt())) {
            collectRequest.setStatus(CollectStatus.EXPIRED);
            collectRequestRepository.save(collectRequest);
            throw new ApiException(HttpStatus.GONE, "Collect request has expired");
        }

        // Execute transfer: "to" pays "from" (toVpa is the payer)
        TransferRequest transferRequest = new TransferRequest();
        transferRequest.setPayerVpa(collectRequest.getToVpa());
        transferRequest.setPayeeVpa(collectRequest.getFromVpa());
        transferRequest.setAmount(collectRequest.getAmount());
        transferRequest.setNote(collectRequest.getNote());
        transferRequest.setUpiPin(approveRequest.getUpiPin());
        transferRequest.setClientRef(collectRequest.getRequestRef());

        var transferResponse = transferService.executeTransfer(idempotencyKey, transferRequest);

        collectRequest.setStatus(CollectStatus.APPROVED);
        collectRequest.setTxnId(transferResponse.getTxnId());
        collectRequest.setIdempotencyKey(idempotencyKey);
        collectRequest = collectRequestRepository.save(collectRequest);

        log.info("Collect request approved: ref={}, txnId={}", collectRequest.getRequestRef(), transferResponse.getTxnId());
        return mapToResponse(collectRequest);
    }

    @Transactional
    public CollectResponse declineCollectRequest(Long requestId) {
        CollectRequest collectRequest = collectRequestRepository.findById(requestId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Collect request not found"));

        if (collectRequest.getStatus() != CollectStatus.PENDING) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Collect request is not pending");
        }

        collectRequest.setStatus(CollectStatus.DECLINED);
        collectRequest = collectRequestRepository.save(collectRequest);

        log.info("Collect request declined: ref={}", collectRequest.getRequestRef());
        return mapToResponse(collectRequest);
    }

    private CollectResponse mapToResponse(CollectRequest req) {
        return CollectResponse.builder()
                .requestId(req.getId())
                .requestRef(req.getRequestRef())
                .fromVpa(req.getFromVpa())
                .toVpa(req.getToVpa())
                .amount(req.getAmount())
                .note(req.getNote())
                .status(req.getStatus())
                .expiresAt(req.getExpiresAt())
                .createdAt(req.getCreatedAt())
                .build();
    }
}
