package com.example.payment.global.exception;

import lombok.Getter;

@Getter
public class DuplicateFieldException extends RuntimeException {
    private final String fieldName;  // 어떤 필드가 중복됐는지 저장 (nickname, email 등)

    public DuplicateFieldException(String fieldName, String message) {
        super(message);
        this.fieldName = fieldName;
    }
}
