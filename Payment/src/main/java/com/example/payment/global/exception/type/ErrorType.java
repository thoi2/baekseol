package com.example.payment.global.exception.type;

public interface ErrorType {
    int getHttpStatusCode();

    String getMessage();
}
