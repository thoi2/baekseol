package com.example.payment.exception;

import com.example.payment.global.exception.type.SuccessType;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public enum PaymentSuccessType implements SuccessType {
    SUCCESS_CREATE_PAYMENT(HttpStatus.CREATED, "결제 요청이 성공했습니다."),
    SUCCESS_INQUIRY_PAYMENT(HttpStatus.OK, "조회가 성공했습니다."),
    SUCCESS_CREATE_WITHDRAWAL(HttpStatus.CREATED, "환급 요청이 성공했습니다.");

    private final HttpStatus httpStatus;
    private final String message;

    @Override
    public int getHttpStatusCode() {
        return httpStatus.value();
    }
}
