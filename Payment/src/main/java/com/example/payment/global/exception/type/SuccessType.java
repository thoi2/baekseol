package com.example.payment.global.exception.type;

/**
 * Represents a type that defines successful outcomes in the application.
 * <p>
 * This interface provides methods to retrieve the HTTP status code and a message
 * associated with a specific success type. Implementations of this interface are
 * intended to standardize and centralize the handling of successful response codes
 * and messages across the application.
 * <p>
 * It is similar in structure to the ErrorType interface, but specifically represents success scenarios.
 */
public interface SuccessType {
    int getHttpStatusCode();

    String getMessage();
}
