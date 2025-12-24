package com.example.payment.global.exception;

import com.example.payment.global.common.ApiResponse;
import com.example.payment.global.exception.type.GlobalErrorType;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.Errors;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import static com.example.payment.global.exception.type.GlobalErrorType.*;
import static org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR;

@RestControllerAdvice
@RequiredArgsConstructor
public class GlobalExceptionHandler {

    /**
     * 400 VALIDATION_ERROR
     */
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ExceptionHandler(MethodArgumentNotValidException.class)
    protected ApiResponse<?> handleMethodArgumentNotValidException(final MethodArgumentNotValidException e) {

        Errors errors = e.getBindingResult();
        Map<String, String> validateDetails = new HashMap<>();

        for (FieldError error : errors.getFieldErrors()) {
            String validKeyName = String.format("valid_%s", error.getField());
            validateDetails.put(validKeyName, error.getDefaultMessage());
        }
        return ApiResponse.error(REQUEST_VALIDATION_ERROR, validateDetails);
    }

    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ExceptionHandler(UnexpectedTypeException.class)
    protected ApiResponse<?> handleUnexprectedTypeException(final UnexpectedTypeException e) {
        return ApiResponse.error(INVALID_TYPE_ERROR);
    }

    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ApiResponse<?> handleMethodArgumentTypeMismatchException(final MethodArgumentTypeMismatchException e) {
        return ApiResponse.error(INVALID_TYPE_ERROR);
    }

    // WebFlux에서도 HttpMessageNotReadableException 은 그대로 쓸 수 있음
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ExceptionHandler(HttpMessageNotReadableException.class)
    protected ApiResponse<?> handleHttpMessageNotReadableException(final HttpMessageNotReadableException e) {
        return ApiResponse.error(INVALID_HTTP_REQUEST_ERROR);
    }

    // HttpRequestMethodNotSupportedException, MissingRequestHeaderException 은 MVC 전용이라 제거


    /**
     * 500 INTERNAL_SERVER_ERROR
     */
    @ResponseStatus(INTERNAL_SERVER_ERROR)
    @ExceptionHandler(Exception.class)
    protected ApiResponse<Exception> handleException(final Exception e) {
        return ApiResponse.error(GlobalErrorType.INTERNAL_SERVER_ERROR, e);
    }

    @ResponseStatus(INTERNAL_SERVER_ERROR)
    @ExceptionHandler(IllegalArgumentException.class)
    public ApiResponse<Exception> handleIllegalArgumentException(final IllegalArgumentException e) {
        return ApiResponse.error(GlobalErrorType.INTERNAL_SERVER_ERROR, e);
    }

    @ResponseStatus(INTERNAL_SERVER_ERROR)
    @ExceptionHandler(IOException.class)
    public ApiResponse<Exception> handleIOException(final IOException e) {
        return ApiResponse.error(GlobalErrorType.INTERNAL_SERVER_ERROR, e);
    }

    @ResponseStatus(INTERNAL_SERVER_ERROR)
    @ExceptionHandler(RuntimeException.class)
    public ApiResponse<Exception> handleRuntimeException(final RuntimeException e) {
        if (e.getMessage() != null) {
            return ApiResponse.error(GlobalErrorType.INTERNAL_SERVER_ERROR, e.getMessage(), e);
        } else {
            return ApiResponse.error(GlobalErrorType.INTERNAL_SERVER_ERROR, e);
        }
    }

    /**
     * CUSTOM_ERROR
     */
    @ExceptionHandler(CustomException.class)
    protected ResponseEntity<ApiResponse<?>> handleCustomException(CustomException e) {
        return ResponseEntity.status(e.getHttpStatus())
                .body(ApiResponse.error(e.getErrorType()));
    }

    /*
     * User Exception
     */
    @ExceptionHandler(DuplicateFieldException.class)
    public ResponseEntity<Map<String, String>> handleExceptionField(DuplicateFieldException e) {
        Map<String, String> error = new HashMap<>();
        error.put("field", e.getFieldName());
        error.put("message", e.getMessage());

        return ResponseEntity.status(HttpStatus.CONFLICT).body(error);
    }

    @ExceptionHandler(LoginFailedException.class)
    public ResponseEntity<Map<String, String>> handleLoginFailed(LoginFailedException e) {
        Map<String, String> error = new HashMap<>();
        error.put("fieldName", e.getFieldName());
        error.put("message", e.getMessage());
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(error);
    }
}
