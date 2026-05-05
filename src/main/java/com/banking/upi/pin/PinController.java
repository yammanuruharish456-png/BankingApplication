package com.banking.upi.pin;

import com.banking.upi.pin.dto.SetPinRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/v1/pin")
@RequiredArgsConstructor
@Tag(name = "PIN", description = "UPI PIN management")
@SecurityRequirement(name = "bearerAuth")
public class PinController {

    private final PinService pinService;

    @PostMapping("/set")
    @Operation(summary = "Set or update UPI PIN")
    public ResponseEntity<Map<String, String>> setPin(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody SetPinRequest request) {
        pinService.setPin(userDetails.getUsername(), request);
        return ResponseEntity.ok(Map.of("message", "PIN set successfully"));
    }
}
