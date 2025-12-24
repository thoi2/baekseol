package com.example.payment.global.exception.type;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public enum GlobalErrorType implements ErrorType {
    // 표준 오류
    REQUEST_VALIDATION_ERROR(HttpStatus.BAD_REQUEST, "잘못된 요청입니다."),
    INVALID_TYPE_ERROR(HttpStatus.BAD_REQUEST, "잘못된 타입이 입력되었습니다."),
    INVALID_MISSING_HEADER_ERROR(HttpStatus.BAD_REQUEST, "요청에 필요한 헤더값이 존재하지 않습니다."),
    INVALID_HTTP_REQUEST_ERROR(HttpStatus.BAD_REQUEST, "요청 형식이 허용된 형식과 다릅니다."),
    INVALID_HTTP_METHOD_ERROR(HttpStatus.BAD_REQUEST, "지원되지 않는 HTTP method 요청입니다."),
    INVALID_TOKEN_HEADER_ERROR(HttpStatus.BAD_REQUEST, "토큰 헤더값의 형식이 잘못되었습니다."),
    INVALID_CODE_ERROR(HttpStatus.BAD_REQUEST, "code 값의 형식이 잘못되었습니다."),
    INVALID_SOCIAL_PLATFORM_ERROR(HttpStatus.BAD_REQUEST, "잘못된 소셜 플랫폼 이름입니다."),
    JSON_PARSING_ERROR(HttpStatus.BAD_REQUEST, "JSON 파싱에 실패했습니다."),
    INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "서버 내부 오류가 발생했습니다."),

    // 인증 관련 오류
    INVALID_JWT_TOKEN(HttpStatus.UNAUTHORIZED, "유효하지 않는 JWT 토큰입니다."),
    EXPIRED_JWT_TOKEN(HttpStatus.UNAUTHORIZED, "만료된 JWT 토큰입니다."),
    UNSUPPORTED_JWT_TOKEN(HttpStatus.UNAUTHORIZED, "지원하지 않는 JWT 토큰입니다."),
    EMPTY_JWT_TOKEN(HttpStatus.UNAUTHORIZED, "JWT 토큰이 존재하지 않습니다."),
    INVALID_JWT_SIGNATURE(HttpStatus.UNAUTHORIZED, "잘못된 JWT 서명입니다."),
    NOT_FOUND_REFRESH_TOKEN_ERROR(HttpStatus.NOT_FOUND, "존재하지 않는 리프레시 토큰입니다."),
    UNKNOWN_JWT_ERROR(HttpStatus.UNAUTHORIZED, "알 수 없는 JWT 토큰 오류가 발생했습니다."),
    EMPTY_PRINCIPLE_ERROR(HttpStatus.BAD_REQUEST, "Principle 객체가 없습니다. (null)"),
    NOT_FOUND_ENTITY(HttpStatus.NOT_FOUND, "해당 데이터가 존재하지 않습니다");


    private final HttpStatus httpStatus;
    private final String message;

    @Override
    public int getHttpStatusCode() {
        return httpStatus.value();
    }
}
