package com.library360.common;

import lombok.Getter;
import org.springframework.http.HttpStatus;

/**
 * Enumeration of all business error codes with associated HTTP status and message.
 */
@Getter
public enum ErrorCode {
    // Auth
    INVALID_CREDENTIALS(HttpStatus.UNAUTHORIZED, "Invalid email or password"),
    ACCOUNT_LOCKED(HttpStatus.FORBIDDEN, "Account is temporarily locked due to too many failed login attempts"),
    ACCOUNT_DISABLED(HttpStatus.FORBIDDEN, "Account is disabled"),
    ACCOUNT_PENDING(HttpStatus.FORBIDDEN, "Your college account is awaiting verification by the library team"),
    INVALID_REFRESH_TOKEN(HttpStatus.UNAUTHORIZED, "Session expired, please log in again"),
    REFRESH_TOKEN_REUSED(HttpStatus.UNAUTHORIZED, "Session revoked due to token reuse detection"),

    // Members
    MEMBER_NOT_FOUND(HttpStatus.NOT_FOUND, "Member not found"),
    MEMBER_INACTIVE(HttpStatus.UNPROCESSABLE_ENTITY, "Member is suspended or expired"),
    MEMBER_CODE_EXISTS(HttpStatus.CONFLICT, "Member code already exists"),

    // Catalog
    BOOK_NOT_FOUND(HttpStatus.NOT_FOUND, "Book not found"),
    ISBN_EXISTS(HttpStatus.CONFLICT, "A book with this ISBN already exists"),
    AUTHOR_NOT_FOUND(HttpStatus.NOT_FOUND, "Author not found"),
    CATEGORY_NOT_FOUND(HttpStatus.NOT_FOUND, "Category not found"),
    PUBLISHER_NOT_FOUND(HttpStatus.NOT_FOUND, "Publisher not found"),

    // Copies
    COPY_NOT_FOUND(HttpStatus.NOT_FOUND, "Copy not found"),
    COPY_BARCODE_EXISTS(HttpStatus.CONFLICT, "A copy with this barcode already exists"),
    COPY_NOT_AVAILABLE(HttpStatus.CONFLICT, "Copy is not available for loan"),
    COPY_ON_HOLD_FOR_ANOTHER(HttpStatus.CONFLICT, "Copy is on hold for another member"),
    COPY_RESERVED_FOR_OTHERS(HttpStatus.CONFLICT, "Last available copy is reserved for queued members"),

    // Loans
    LOAN_NOT_FOUND(HttpStatus.NOT_FOUND, "Loan not found"),
    NO_OPEN_LOAN(HttpStatus.CONFLICT, "No open loan found for this copy"),
    LOAN_LIMIT_REACHED(HttpStatus.UNPROCESSABLE_ENTITY, "Maximum loan limit reached"),
    FINES_OUTSTANDING(HttpStatus.UNPROCESSABLE_ENTITY, "Outstanding fines exceed the allowed threshold"),
    RENEWAL_LIMIT_REACHED(HttpStatus.UNPROCESSABLE_ENTITY, "Maximum renewal limit reached"),
    TITLE_HAS_RESERVATIONS(HttpStatus.UNPROCESSABLE_ENTITY, "Cannot renew: others are waiting for this title"),
    LOAN_OVERDUE(HttpStatus.UNPROCESSABLE_ENTITY, "Overdue loans cannot be renewed"),

    // Reservations
    RESERVATION_NOT_FOUND(HttpStatus.NOT_FOUND, "Reservation not found"),
    RESERVATION_LIMIT_REACHED(HttpStatus.UNPROCESSABLE_ENTITY, "Maximum reservation limit reached"),
    ALREADY_RESERVED(HttpStatus.CONFLICT, "You already have an active reservation for this title"),
    ALREADY_BORROWED(HttpStatus.CONFLICT, "You already have this title on loan"),
    COPIES_AVAILABLE(HttpStatus.UNPROCESSABLE_ENTITY, "Copies are available; no need to reserve"),

    // Fines
    FINE_NOT_FOUND(HttpStatus.NOT_FOUND, "Fine not found"),
    FINE_ALREADY_SETTLED(HttpStatus.CONFLICT, "Fine is already paid or waived"),
    PAYMENT_EXCEEDS_BALANCE(HttpStatus.UNPROCESSABLE_ENTITY, "Payment amount exceeds outstanding balance"),

    // Users
    USER_NOT_FOUND(HttpStatus.NOT_FOUND, "User not found"),
    EMAIL_EXISTS(HttpStatus.CONFLICT, "An account with this email already exists"),

    // Generic
    ACCESS_DENIED(HttpStatus.FORBIDDEN, "You do not have permission to perform this action"),
    CONCURRENT_MODIFICATION(HttpStatus.CONFLICT, "Record was modified by someone else. Please refresh and retry.");

    private final HttpStatus status;
    private final String message;

    ErrorCode(HttpStatus status, String message) {
        this.status = status;
        this.message = message;
    }
}
