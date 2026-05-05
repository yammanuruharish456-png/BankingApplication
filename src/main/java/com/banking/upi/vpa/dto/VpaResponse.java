package com.banking.upi.vpa.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VpaResponse {
    private Long id;
    private String handle;
    private Long userId;
    private Long accountId;
    private Boolean active;
}
