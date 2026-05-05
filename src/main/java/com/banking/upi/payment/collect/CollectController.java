package com.banking.upi.payment.collect;

import com.banking.upi.payment.collect.dto.ApproveRequest;
import com.banking.upi.payment.collect.dto.CollectRequestDto;
import com.banking.upi.payment.collect.dto.CollectResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/v1/collect")
@RequiredArgsConstructor
@Tag(name = "Collect", description = "Request money (collect) endpoints")
@SecurityRequirement(name = "bearerAuth")
public class CollectController {

    private final CollectService collectService;

    @PostMapping
    @Operation(summary = "Create a collect (request money) request")
    public ResponseEntity<CollectResponse> createCollect(@Valid @RequestBody CollectRequestDto request) {
        return ResponseEntity.ok(collectService.createCollectRequest(request));
    }

    @PostMapping("/{requestId}/approve")
    @Operation(summary = "Approve a collect request")
    public ResponseEntity<CollectResponse> approve(
            @PathVariable Long requestId,
            @RequestHeader("Idempotency-Key") String idempotencyKey,
            @Valid @RequestBody ApproveRequest request) {
        return ResponseEntity.ok(collectService.approveCollectRequest(requestId, idempotencyKey, request));
    }

    @PostMapping("/{requestId}/decline")
    @Operation(summary = "Decline a collect request")
    public ResponseEntity<CollectResponse> decline(@PathVariable Long requestId) {
        return ResponseEntity.ok(collectService.declineCollectRequest(requestId));
    }
}
