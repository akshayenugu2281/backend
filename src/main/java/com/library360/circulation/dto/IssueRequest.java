package com.library360.circulation.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record IssueRequest(
    @NotNull(message = "Member ID is required") 
    Long memberId,
    
    @NotBlank(message = "Barcode is required") 
    @Size(max = 40, message = "Barcode must be at most 40 characters") 
    String barcode
) {}
