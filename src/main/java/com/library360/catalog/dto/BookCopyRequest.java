package com.library360.catalog.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record BookCopyRequest(
    @NotBlank(message = "Barcode is required")
    @Pattern(regexp = "[A-Za-z0-9][A-Za-z0-9._-]{0,39}", message = "Barcode must be 1-40 letters, digits, dots, underscores, or hyphens")
    String barcode,

    @Size(max = 40, message = "Shelf must be at most 40 characters")
    String shelf,

    @Size(max = 300, message = "Notes must be at most 300 characters")
    String notes
) {}
