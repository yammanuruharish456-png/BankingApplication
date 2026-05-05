package com.banking.upi.vpa;

import com.banking.upi.vpa.dto.CreateVpaRequest;
import com.banking.upi.vpa.dto.VpaResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/v1/vpa")
@RequiredArgsConstructor
@Tag(name = "VPA", description = "Virtual Payment Address management")
@SecurityRequirement(name = "bearerAuth")
public class VpaController {

    private final VpaService vpaService;

    @PostMapping
    @Operation(summary = "Create a new VPA")
    public ResponseEntity<VpaResponse> createVpa(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody CreateVpaRequest request) {
        return ResponseEntity.ok(vpaService.createVpa(userDetails.getUsername(), request));
    }

    @GetMapping("/{handle}")
    @Operation(summary = "Resolve a VPA handle")
    public ResponseEntity<VpaResponse> resolveVpa(@PathVariable String handle) {
        return ResponseEntity.ok(vpaService.resolveVpa(handle));
    }
}
