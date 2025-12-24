package com.example.payment.controller;

import com.example.payment.dto.PaymentRequest;
import com.example.payment.dto.WithdrawalRequestDto;
import com.example.payment.global.common.ApiResponse;
import com.example.payment.service.PaymentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import static com.example.payment.exception.PaymentSuccessType.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/payments")
@Slf4j
public class PaymentController {

    private final PaymentService paymentService;

    // ✅ principal = String userId 이므로 Long 변환만 도와주는 헬퍼
    private Long toUserId(String userId) {
        return Long.parseLong(userId);
    }

    // ================== 결제 관련 ==================

    // 결제 요청 (POST /pay)
    @PostMapping("/pay")
    public Mono<ResponseEntity<ApiResponse<?>>> payment(
            @AuthenticationPrincipal String userId,
            @RequestBody PaymentRequest req
    ) {
        Long uid = toUserId(userId);
        log.info("결제 요청: userId={}, amount={}", uid, req.getAmount());

        return paymentService.requestPayment(
                        uid,
                        req.getAmount(),
                        req.getOrderId(),
                        req.getOrderName(),
                        req.getPaymentKey()
                )
                .map(result -> ResponseEntity
                        .status(SUCCESS_CREATE_PAYMENT.getHttpStatusCode())
                        .body(ApiResponse.success(SUCCESS_CREATE_PAYMENT, result)));
    }

    // 결제 상세 조회 - 본인만 (GET /pay/{paymentId})
    @GetMapping("/pay/{paymentId}")
    public Mono<ResponseEntity<ApiResponse<?>>> getPayment(
            @AuthenticationPrincipal String userId,
            @PathVariable Long paymentId
    ) {
        Long uid = toUserId(userId);
        log.info("결제 조회: userId={}, paymentId={}", uid, paymentId);

        return paymentService.getPayment(uid, paymentId)
                .map(result -> ResponseEntity
                        .status(SUCCESS_INQUIRY_PAYMENT.getHttpStatusCode())
                        .body(ApiResponse.success(SUCCESS_INQUIRY_PAYMENT, result)));
    }

    // 사용자 결제 목록 조회 - 본인만 (GET /pay/user)
    @GetMapping("/pay/user")
    public Flux<ResponseEntity<ApiResponse<?>>> getPaymentList(
            @AuthenticationPrincipal String userId
    ) {
        Long uid = toUserId(userId);
        log.info("결제 목록 조회: userId={}", uid);

        return paymentService.getPaymentList(uid)
                .map(result -> ResponseEntity
                        .status(SUCCESS_INQUIRY_PAYMENT.getHttpStatusCode())
                        .body(ApiResponse.success(SUCCESS_INQUIRY_PAYMENT, result)));
    }

    // ================== 환급 관련 ==================

    // 환급 요청 (POST /withdrawal)
    @PostMapping("/withdrawal")
    public Mono<ResponseEntity<ApiResponse<?>>> withdrawal(
            @AuthenticationPrincipal String userId,
            @RequestBody WithdrawalRequestDto req
    ) {
        Long uid = toUserId(userId);
        log.info("환급 요청: userId={}, amount={}", uid, req.getAmount());

        return paymentService.requestWithdrawal(
                        uid,
                        req.getAmount(),
                        req.getBankCode(),
                        req.getAccount()
                )
                .map(result -> ResponseEntity
                        .status(SUCCESS_CREATE_WITHDRAWAL.getHttpStatusCode())
                        .body(ApiResponse.success(SUCCESS_CREATE_WITHDRAWAL, result)));
    }

    // 환급 상세 조회 - 본인만 (GET /withdrawal/{withdrawalId})
    @GetMapping("/withdrawal/{withdrawalId}")
    public Mono<ResponseEntity<ApiResponse<?>>> getWithdrawal(
            @AuthenticationPrincipal String userId,
            @PathVariable Long withdrawalId
    ) {
        Long uid = toUserId(userId);
        log.info("환급 조회: userId={}, withdrawalId={}", uid, withdrawalId);

        return paymentService.getWithdrawal(uid, withdrawalId)
                .map(result -> ResponseEntity
                        .status(SUCCESS_INQUIRY_PAYMENT.getHttpStatusCode())
                        .body(ApiResponse.success(SUCCESS_INQUIRY_PAYMENT, result)));
    }

    // 사용자 환급 목록 조회 - 본인만 (GET /withdrawal/user)
    @GetMapping("/withdrawal/user")
    public Flux<ResponseEntity<ApiResponse<?>>> getWithdrawalList(
            @AuthenticationPrincipal String userId
    ) {
        Long uid = toUserId(userId);
        log.info("환급 목록 조회: userId={}", uid);

        return paymentService.getWithdrawalList(uid)
                .map(result -> ResponseEntity
                        .status(SUCCESS_INQUIRY_PAYMENT.getHttpStatusCode())
                        .body(ApiResponse.success(SUCCESS_INQUIRY_PAYMENT, result)));
    }
}
