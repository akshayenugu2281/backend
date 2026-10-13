package com.library360.catalog.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.util.List;

public record BookRequest(
    @Pattern(regexp = "^$|\\d{13}", message = "ISBN must contain exactly 13 digits")
    String isbn13,

    @NotBlank(message = "Title is required")
    @Size(max = 255, message = "Title must be at most 255 characters")
    String title,

    @Size(max = 255)
    String subtitle,

    @Size(max = 40)
    String edition,

    @Min(value = 1000, message = "Publication year must be 1000 or later")
    @Max(value = 2200, message = "Publication year must be 2200 or earlier")
    Integer publishYear,

    @Size(max = 10)
    String language,

    @Positive(message = "Page count must be positive")
    Integer pages,

    String description,

    @Size(max = 500)
    String coverUrl,

    Long categoryId,
    Long publisherId,
    List<Long> authorIds,

    // Optional physical inventory for initial book creation. Ignored during updates.
    @Pattern(regexp = "^$|[A-Za-z0-9][A-Za-z0-9._-]{0,39}", message = "Barcode must be 1-40 letters, digits, dots, underscores, or hyphens")
    String initialCopyBarcode,

    @Size(max = 40, message = "Shelf must be at most 40 characters")
    String initialCopyShelf
) {}
