// payment/controller/PaymentController.java
package com.example.backend.payment.controller;

import com.example.backend.global.common.ApiResponse;
import com.example.backend.payment.dto.PaymentRequest;
import com.example.backend.payment.dto.WithdrawalRequestDto;
import com.example.backend.payment.service.PaymentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.security.Principal;
import static com.example.backend.payment.exception.PaymentSuccessType.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/payments")
@Slf4j
public class PaymentController {

    private final PaymentService paymentService;

    private Long userId(Principal principal) {
        return Long.parseLong(principal.getName());
    }

    // ✅ 결제 요청
    @PostMapping("/pay")
    public ResponseEntity<ApiResponse<?>> payment(
            Principal principal,
            @RequestBody PaymentRequest req) {

        log.info("결제 요청: userId={}, amount={}", userId(principal), req.getAmount());

        return ResponseEntity
                .status(SUCCESS_CREATE_PAYMENT.getHttpStatusCode())
                .body(ApiResponse.success(
                        SUCCESS_CREATE_PAYMENT,
                        paymentService.requestPayment(
                                userId(principal),
                                req.getAmount(),
                                req.getOrderId(),
                                req.getOrderName(),
                                req.getPaymentKey()
                        )
                ));
    }

    // ✅ 결제 상세 조회 - 본인만
    @GetMapping("/pay/{paymentId}")
    public ResponseEntity<ApiResponse<?>> getPayment(
            Principal principal,
            @PathVariable Long paymentId) {

        Long userId = userId(principal);
        log.info("결제 조회: userId={}, paymentId={}", userId, paymentId);

        return ResponseEntity
                .status(SUCCESS_INQUIRY_PAYMENT.getHttpStatusCode())
                .body(ApiResponse.success(
                        SUCCESS_INQUIRY_PAYMENT,
                        paymentService.getPayment(userId, paymentId)
                ));
    }

    // ✅ 사용자 결제 목록 조회 - 본인만
    @GetMapping("/pay/user")
    public ResponseEntity<ApiResponse<?>> getPaymentList(
            Principal principal) {

        Long userId = userId(principal);
        log.info("결제 목록 조회: userId={}", userId);

        return ResponseEntity
                .status(SUCCESS_INQUIRY_PAYMENT.getHttpStatusCode())
                .body(ApiResponse.success(
                        SUCCESS_INQUIRY_PAYMENT,
                        paymentService.getPaymentList(userId)
                ));
    }

    // ✅ 환급 요청
    @PostMapping("/withdrawal")
    public ResponseEntity<ApiResponse<?>> withdrawal(
            Principal principal,
            @RequestBody WithdrawalRequestDto req) {

        log.info("환급 요청: userId={}, amount={}", userId(principal), req.getAmount());

        return ResponseEntity
                .status(SUCCESS_CREATE_WITHDRAWAL.getHttpStatusCode())
                .body(ApiResponse.success(
                        SUCCESS_CREATE_WITHDRAWAL,
                        paymentService.requestWithdrawal(
                                userId(principal),
                                req.getAmount(),
                                req.getBankCode(),
                                req.getAccount()
                        )
                ));
    }

    // ✅ 환급 상세 조회 - 본인만
    @GetMapping("/withdrawal/{withdrawalId}")
    public ResponseEntity<ApiResponse<?>> getWithdrawal(
            Principal principal,
            @PathVariable Long withdrawalId) {

        Long userId = userId(principal);
        log.info("환급 조회: userId={}, withdrawalId={}", userId, withdrawalId);

        return ResponseEntity
                .status(SUCCESS_INQUIRY_PAYMENT.getHttpStatusCode())
                .body(ApiResponse.success(
                        SUCCESS_INQUIRY_PAYMENT,
                        paymentService.getWithdrawal(userId, withdrawalId)
                ));
    }

    // ✅ 사용자 환급 목록 조회 - 본인만
    @GetMapping("/withdrawal/user")
    public ResponseEntity<ApiResponse<?>> getWithdrawalList(
            Principal principal) {

        Long userId = userId(principal);
        log.info("환급 목록 조회: userId={}", userId);

        return ResponseEntity
                .status(SUCCESS_INQUIRY_PAYMENT.getHttpStatusCode())
                .body(ApiResponse.success(
                        SUCCESS_INQUIRY_PAYMENT,
                        paymentService.getWithdrawalList(userId)
                ));
    }
}
