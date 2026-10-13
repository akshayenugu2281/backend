package com.library360.circulation.dto;

import com.library360.fine.dto.FineResponse;

public record ReturnResponse(
    LoanResponse loan,
    FineResponse fine // Can be null if no fine was generated
) {}
