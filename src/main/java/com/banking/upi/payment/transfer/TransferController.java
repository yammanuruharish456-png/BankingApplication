package com.banking.upi.payment.transfer;

import com.banking.upi.payment.transfer.dto.TransferRequest;
import com.banking.upi.payment.transfer.dto.TransferResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/v1/payments")
@RequiredArgsConstructor
@Tag(name = "Payments", description = "UPI payment transfers")
@SecurityRequirement(name = "bearerAuth")
public class TransferController {

    private final TransferService transferService;

    @PostMapping("/transfer")
    @Operation(summary = "Execute a UPI transfer")
    public ResponseEntity<TransferResponse> transfer(
            @RequestHeader("Idempotency-Key") String idempotencyKey,
            @Valid @RequestBody TransferRequest request) {
        return ResponseEntity.ok(transferService.executeTransfer(idempotencyKey, request));
    }

    @GetMapping("/{txnId}")
    @Operation(summary = "Get transaction by ID")
    public ResponseEntity<TransferResponse> getTransaction(@PathVariable Long txnId) {
        return ResponseEntity.ok(transferService.getTransaction(txnId));
    }
}
