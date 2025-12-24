package com.example.payment.exception;

import com.example.payment.global.exception.type.ErrorType;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public enum PaymentErrorType implements ErrorType {

    // ===================== 결제 관련 (4xx Bad Request) =====================
    ERROR_PAYMENT_KEY_EMPTY(HttpStatus.BAD_REQUEST, "결제키(paymentKey)가 비어있습니다"),
    ERROR_PAYMENT_ALREADY_CONFIRMED(HttpStatus.BAD_REQUEST, "이미 승인된 결제입니다"),
    ERROR_PAYMENT_IN_PROCESSING(HttpStatus.BAD_REQUEST, "현재 결제 처리 중입니다. 잠시 후 다시 시도해주세요"),
    ERROR_PAYMENT_FAILED_RETRY(HttpStatus.BAD_REQUEST, "이전 결제가 실패하여 재시도 중입니다"),
    ERROR_PAYMENT_CONFIRM_FAILED(HttpStatus.BAD_REQUEST, "결제 승인 실패"),
    ERROR_PAYMENT_INVALID_AMOUNT(HttpStatus.BAD_REQUEST, "유효하지 않은 결제 금액입니다"),
    ERROR_PAYMENT_API_FAILED(HttpStatus.BAD_REQUEST, "외부 결제 API 호출 실패"),

    // ===================== 환급 관련 (4xx Bad Request) =====================
    ERROR_WITHDRAWAL_INSUFFICIENT_POINTS(HttpStatus.BAD_REQUEST, "포인트가 부족하여 환급할 수 없습니다"),
    ERROR_WITHDRAWAL_INVALID_AMOUNT(HttpStatus.BAD_REQUEST, "유효하지 않은 환급 금액입니다"),
    ERROR_WITHDRAWAL_INVALID_BANK_CODE(HttpStatus.BAD_REQUEST, "유효하지 않은 은행 코드입니다"),
    ERROR_WITHDRAWAL_INVALID_ACCOUNT(HttpStatus.BAD_REQUEST, "유효하지 않은 계좌번호입니다"),
    ERROR_WITHDRAWAL_FEE_EXCEEDS_AMOUNT(HttpStatus.BAD_REQUEST, "환급 수수료가 환급 금액을 초과합니다"),
    ERROR_WITHDRAWAL_PROCESSING_FAILED(HttpStatus.BAD_REQUEST, "환급 처리 중 오류가 발생했습니다"),
    WITHDRAWAL_NOT_FOUND(HttpStatus.NOT_FOUND, "환급 내역을 찾을 수 없습니다."),


    // ===================== 사용자 관련 (4xx Not Found) =====================
    ERROR_USER_NOT_FOUND(HttpStatus.NOT_FOUND, "사용자를 찾을 수 없습니다"),
    ERROR_PAYMENT_NOT_FOUND(HttpStatus.NOT_FOUND, "결제 정보를 찾을 수 없습니다"),
    ERROR_WITHDRAWAL_NOT_FOUND(HttpStatus.NOT_FOUND, "환급 정보를 찾을 수 없습니다"),
    ERROR_PASSWORD_REQUIRED(HttpStatus.NOT_FOUND, "비밀번호를 입력해주세요"),
    ERROR_PASSWORD_INCORRECT(HttpStatus.NOT_FOUND, "비밀번호가 올바르지 않습니다."),

    // ===================== 권한 관련 (4xx Forbidden) =====================
    ERROR_PAYMENT_UNAUTHORIZED(HttpStatus.FORBIDDEN, "결제 정보에 접근할 권한이 없습니다"),
    ERROR_WITHDRAWAL_UNAUTHORIZED(HttpStatus.FORBIDDEN, "환급 정보에 접근할 권한이 없습니다"),
    UNAUTHORIZED_ACCESS(HttpStatus.FORBIDDEN, "접근 권한이 없습니다."),
    // ===================== 일반 에러 =====================
    ERROR_CREATE_PAYMENT(HttpStatus.BAD_REQUEST, "결제 요청 중 오류가 발생했습니다"),
    ERROR_CREATE_WITHDRAWAL(HttpStatus.BAD_REQUEST, "환급 요청 중 오류가 발생했습니다"),
    ERROR_INSUFFICIENT_POINTS(HttpStatus.BAD_REQUEST, "포인트가 부족합니다");

    private final HttpStatus httpStatus;
    private final String message;

    @Override
    public int getHttpStatusCode() {
        return httpStatus.value();
    }
}
