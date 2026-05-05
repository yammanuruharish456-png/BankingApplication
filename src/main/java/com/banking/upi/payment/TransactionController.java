package com.banking.upi.payment;

import com.banking.upi.domain.Transaction;
import com.banking.upi.domain.User;
import com.banking.upi.domain.Vpa;
import com.banking.upi.exception.ApiException;
import com.banking.upi.payment.transfer.dto.TransferResponse;
import com.banking.upi.repository.TransactionRepository;
import com.banking.upi.repository.UserRepository;
import com.banking.upi.repository.VpaRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/v1/users/me")
@RequiredArgsConstructor
@Tag(name = "Transactions", description = "Transaction history")
@SecurityRequirement(name = "bearerAuth")
public class TransactionController {

    private final TransactionRepository transactionRepository;
    private final UserRepository userRepository;
    private final VpaRepository vpaRepository;

    @GetMapping("/transactions")
    @Operation(summary = "Get my transaction history")
    public ResponseEntity<Page<TransferResponse>> getMyTransactions(
            @AuthenticationPrincipal UserDetails userDetails,
            @PageableDefault(size = 20) Pageable pageable) {

        User user = userRepository.findByMobile(userDetails.getUsername())
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "User not found"));

        List<Vpa> vpas = vpaRepository.findByUserId(user.getId());
        if (vpas.isEmpty()) {
            return ResponseEntity.ok(Page.empty(pageable));
        }

        // Use the first VPA for simplicity; in production, aggregate across all VPAs
        String vpaHandle = vpas.get(0).getHandle();
        Page<Transaction> transactions = transactionRepository.findByPayerVpaOrPayeeVpa(
                vpaHandle, vpaHandle, pageable);

        Page<TransferResponse> responses = transactions.map(txn -> TransferResponse.builder()
                .txnId(txn.getId())
                .txnRef(txn.getTxnRef())
                .status(txn.getStatus())
                .amount(txn.getAmount())
                .payerVpa(txn.getPayerVpa())
                .payeeVpa(txn.getPayeeVpa())
                .note(txn.getNote())
                .createdAt(txn.getCreatedAt())
                .build());

        return ResponseEntity.ok(responses);
    }
}
