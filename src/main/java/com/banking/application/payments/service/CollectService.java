package com.banking.application.payments.service;

import com.banking.application.common.exception.ApiException;
import com.banking.application.payments.dto.*;
import com.banking.application.payments.entity.CollectRequest;
import com.banking.application.payments.entity.CollectRequestStatus;
import com.banking.application.users.entity.User;
import com.banking.application.users.repository.VpaHandleRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.banking.application.payments.repository.CollectRequestRepository;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

@Service
public class CollectService {
    private final CollectRequestRepository collectRequestRepository;
    private final TransferService transferService;
    private final VpaHandleRepository vpaHandleRepository;

    public CollectService(CollectRequestRepository collectRequestRepository,
                          TransferService transferService,
                          VpaHandleRepository vpaHandleRepository) {
        this.collectRequestRepository = collectRequestRepository;
        this.transferService = transferService;
        this.vpaHandleRepository = vpaHandleRepository;
    }

    @Transactional
    public CollectResponse create(User caller, CollectCreateRequest request) {
        String callerVpa = vpaHandleRepository.findFirstByUserId(caller.getId())
                .orElseThrow(() -> new ApiException(HttpStatus.BAD_REQUEST, "User has no VPA"))
                .getVpa();
        if (!callerVpa.equalsIgnoreCase(request.toVpa())) {
            throw new ApiException(HttpStatus.FORBIDDEN, "toVpa must belong to authenticated user");
        }

        CollectRequest collectRequest = new CollectRequest();
        collectRequest.setFromVpa(request.fromVpa());
        collectRequest.setToVpa(request.toVpa());
        collectRequest.setAmount(request.amount());
        collectRequest.setNote(request.note());
        collectRequest.setStatus(CollectRequestStatus.REQUESTED);
        collectRequest.setExpiresAt(Instant.now().plus(1, ChronoUnit.DAYS));
        collectRequest = collectRequestRepository.save(collectRequest);
        return new CollectResponse(collectRequest.getId(), collectRequest.getStatus().name());
    }

    @Transactional
    public TransferResponse approve(User caller, Long requestId, String idempotencyKey, CollectApproveRequest request) {
        CollectRequest collectRequest = collectRequestRepository.findById(requestId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Collect request not found"));

        if (collectRequest.getStatus() != CollectRequestStatus.REQUESTED) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Collect request already processed");
        }
        if (collectRequest.getExpiresAt().isBefore(Instant.now())) {
            collectRequest.setStatus(CollectRequestStatus.EXPIRED);
            collectRequestRepository.save(collectRequest);
            throw new ApiException(HttpStatus.BAD_REQUEST, "Collect request expired");
        }

        String callerVpa = vpaHandleRepository.findFirstByUserId(caller.getId())
                .orElseThrow(() -> new ApiException(HttpStatus.BAD_REQUEST, "User has no VPA"))
                .getVpa();

        if (!callerVpa.equalsIgnoreCase(collectRequest.getFromVpa())) {
            throw new ApiException(HttpStatus.FORBIDDEN, "Only payer can approve collect request");
        }

        TransferRequest transferRequest = new TransferRequest(
                collectRequest.getFromVpa(),
                collectRequest.getToVpa(),
                collectRequest.getAmount(),
                collectRequest.getNote(),
                request.upiPin(),
                "collect-" + collectRequest.getId()
        );
        TransferResponse response = transferService.transfer(
                caller,
                idempotencyKey,
                transferRequest,
                "POST:/v1/collect/{requestId}/approve"
        );
        collectRequest.setStatus(CollectRequestStatus.APPROVED);
        collectRequestRepository.save(collectRequest);
        return response;
    }

    @Transactional
    public CollectResponse decline(User caller, Long requestId) {
        CollectRequest collectRequest = collectRequestRepository.findById(requestId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Collect request not found"));
        String callerVpa = vpaHandleRepository.findFirstByUserId(caller.getId())
                .orElseThrow(() -> new ApiException(HttpStatus.BAD_REQUEST, "User has no VPA"))
                .getVpa();

        if (!callerVpa.equalsIgnoreCase(collectRequest.getFromVpa())) {
            throw new ApiException(HttpStatus.FORBIDDEN, "Only payer can decline collect request");
        }

        if (collectRequest.getStatus() != CollectRequestStatus.REQUESTED) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Collect request already processed");
        }
        collectRequest.setStatus(CollectRequestStatus.DECLINED);
        collectRequestRepository.save(collectRequest);
        return new CollectResponse(collectRequest.getId(), collectRequest.getStatus().name());
    }
}
