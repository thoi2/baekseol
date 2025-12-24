package com.example.payment.service;

import com.example.payment.config.PaymentClient;
import com.example.payment.dto.PaymentResponseDto;
import com.example.payment.dto.WithdrawalResponseDto;
import com.example.payment.entity.Payment;
import com.example.payment.entity.PaymentEvent;
import com.example.payment.entity.WithdrawalRequest;
import com.example.payment.entity.WithdrawalTransaction;
import com.example.payment.enumType.TransactionStatus;
import com.example.payment.global.exception.CustomException;
import com.example.payment.kafka.PaymentEventProducer;
import com.example.payment.repository.PaymentEventRepository;
import com.example.payment.repository.PaymentRepository;
import com.example.payment.repository.WithdrawalRequestRepository;
import com.example.payment.repository.WithdrawalTransactionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.json.simple.JSONObject;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.Map;

import static com.example.payment.exception.PaymentErrorType.*;

@Service
@Slf4j
@RequiredArgsConstructor
public class PaymentService {

    private final PaymentClient paymentClient;
    private final PaymentRepository paymentRepository;
    private final PaymentEventRepository paymentEventRepository;
    private final WithdrawalRequestRepository withdrawalRepository;
    private final WithdrawalTransactionRepository withdrawalTransactionRepository;
    private final PaymentEventProducer paymentEventProducer;

    // ===================== 결제 =====================

    @Transactional
    public Mono<PaymentResponseDto> requestPayment(Long userId, Long amount,
                                                   String orderId, String orderName,
                                                   String paymentKey) {

        if (paymentKey == null || paymentKey.trim().isEmpty()) {
            return Mono.error(new CustomException(ERROR_PAYMENT_KEY_EMPTY));
        }

        if (amount == null || amount <= 0) {
            return Mono.error(new CustomException(ERROR_PAYMENT_INVALID_AMOUNT));
        }

        return paymentRepository.findByPaymentKey(paymentKey)
                .flatMap(this::handleExistingPayment)
                .switchIfEmpty(createNewPayment(userId, amount, orderId, orderName, paymentKey));
    }

    private Mono<PaymentResponseDto> handleExistingPayment(Payment existingPayment) {

        TransactionStatus current = TransactionStatus.valueOf(existingPayment.getStatus());

        if (current == TransactionStatus.CONFIRMED) {
            return Mono.error(new CustomException(ERROR_PAYMENT_ALREADY_CONFIRMED));
        } else if (current == TransactionStatus.PROCESSING) {
            return Mono.error(new CustomException(ERROR_PAYMENT_IN_PROCESSING));
        } else if (current == TransactionStatus.FAILED) {
            log.info("이전 실패한 결제를 재처리: paymentId={}", existingPayment.getPaymentId());
            existingPayment.resetForRetry();

            return paymentRepository.save(existingPayment)
                    .flatMap(saved ->
                            recordPaymentEvent(saved, TransactionStatus.PENDING,
                                    "결제 재시도 요청됨", null)
                                    .then(processPaymentAsync(saved))
                                    .thenReturn(PaymentResponseDto.from(saved)));
        }

        return Mono.just(PaymentResponseDto.from(existingPayment));
    }

    private Mono<PaymentResponseDto> createNewPayment(Long userId, Long amount,
                                                      String orderId, String orderName,
                                                      String paymentKey) {

        Payment payment = Payment.builder()
                .userId(userId)
                .orderId(orderId)
                .amount(amount)
                .paymentKey(paymentKey)
                .status(TransactionStatus.PENDING.name())
                .tossOrderName(orderName)
                .build();

        return paymentRepository.save(payment)
                .flatMap(saved ->
                        recordPaymentEvent(saved, TransactionStatus.PENDING,
                                "결제 요청 생성됨 (결제금액=" + amount + "원)", null)
                                .then(processPaymentAsync(saved))
                                .then(Mono.fromSupplier(() -> {
                                    log.info("결제 요청 생성: paymentId={}, userId={}, amount={}",
                                            saved.getPaymentId(), userId, amount);
                                    return PaymentResponseDto.from(saved);
                                })));
    }

    private Mono<Void> processPaymentAsync(Payment payment) {

        Long paymentId = payment.getPaymentId();

        JSONObject body = new JSONObject();
        body.put("paymentKey", payment.getPaymentKey());
        body.put("amount", payment.getAmount());
        body.put("orderId", payment.getOrderId());
        body.put("orderName", payment.getTossOrderName());

        return Mono.defer(() -> {
                    payment.startProcessing(payment.getPaymentKey());
                    return paymentRepository.save(payment);
                })
                .flatMap(saved ->
                        recordPaymentEvent(saved, TransactionStatus.PROCESSING,
                                "결제 처리 시작됨", null)
                                .doOnSuccess(v ->
                                        log.info("결제 처리 시작: paymentId={}", paymentId)))
                .then(paymentClient.confirmPayment(body))
                .flatMap(response -> {

                    String status = (String) response.get("status");

                    if (!"DONE".equals(status)) {
                        return Mono.error(new CustomException(ERROR_PAYMENT_CONFIRM_FAILED));
                    }

                    String receiptUrl = (String) ((Map<?, ?>) response.get("receipt")).get("url");
                    String method = (String) response.get("method");

                    payment.confirmPayment(
                            LocalDateTime.now(),
                            LocalDateTime.now(),
                            receiptUrl,
                            method
                    );

                    return paymentRepository.save(payment);
                })
                .flatMap(saved -> {
                    log.info("결제 승인: paymentId={}", paymentId);

                    return paymentEventProducer.sendPaymentConfirmedEvent(saved)
                            .then(recordPaymentEvent(saved, TransactionStatus.CONFIRMED,
                                    "결제 승인 완료 (Kafka 이벤트 발행)", null));
                })
                .onErrorResume(e -> {
                    log.error("결제 처리 오류: paymentId={}, error={}", paymentId, e.getMessage(), e);
                    payment.failPayment();
                    return paymentRepository.save(payment)
                            .then(recordPaymentEvent(payment, TransactionStatus.FAILED,
                                    "결제 실패", e.getMessage()))
                            .then();
                })
                .then();
    }

    public Mono<PaymentResponseDto> getPayment(Long userId, Long paymentId) {
        return paymentRepository.findById(paymentId)
                .switchIfEmpty(Mono.error(new CustomException(ERROR_PAYMENT_NOT_FOUND)))
                .flatMap(payment -> {
                    if (!payment.getUserId().equals(userId)) {
                        return Mono.error(new CustomException(UNAUTHORIZED_ACCESS));
                    }
                    return Mono.just(PaymentResponseDto.from(payment));
                });
    }

    public Flux<PaymentResponseDto> getPaymentList(Long userId) {
        return paymentRepository.findByUserIdOrderByCreatedAtDesc(userId)
                .doOnNext(p -> log.info("결제 목록 조회: userId={}, paymentId={}",
                        userId, p.getPaymentId()))
                .map(PaymentResponseDto::from);
    }

    // ===================== 환급 =====================

    @Transactional
    public Mono<WithdrawalResponseDto> requestWithdrawal(Long userId, Long amount,
                                                         String bankCode, String account) {

        if (amount == null || amount <= 0) {
            return Mono.error(new CustomException(ERROR_WITHDRAWAL_INVALID_AMOUNT));
        }

        if (bankCode == null || bankCode.trim().isEmpty()) {
            return Mono.error(new CustomException(ERROR_WITHDRAWAL_INVALID_BANK_CODE));
        }

        if (account == null || account.trim().isEmpty()) {
            return Mono.error(new CustomException(ERROR_WITHDRAWAL_INVALID_ACCOUNT));
        }

        WithdrawalRequest withdrawal = WithdrawalRequest.builder()
                .userId(userId)
                .amount(amount)
                .bankCode(bankCode)
                .account(account)
                .status(TransactionStatus.PENDING.name())
                .build();

        return withdrawalRepository.save(withdrawal)
                .flatMap(saved ->
                        recordWithdrawalEvent(saved.getWithdrawalId(), TransactionStatus.PENDING,
                                "환급 요청 생성됨", null)
                                .then(processWithdrawalAsync(saved))
                                .thenReturn(WithdrawalResponseDto.from(saved))
                                .doOnSuccess(dto ->
                                        log.info("환급 요청: withdrawalId={}, userId={}, amount={}",
                                                saved.getWithdrawalId(), userId, amount)));
    }

    private Mono<Void> processWithdrawalAsync(WithdrawalRequest withdrawal) {

        Long wid = withdrawal.getWithdrawalId();

        return Mono.defer(() -> {
                    withdrawal.startProcessing();
                    return withdrawalRepository.save(withdrawal);
                })
                .flatMap(saved -> {

                    JSONObject processingLog = new JSONObject();
                    processingLog.put("event", "환급 처리 시작");
                    processingLog.put("status", "PROCESSING");
                    processingLog.put("timestamp", System.currentTimeMillis());

                    WithdrawalTransaction processingTran = WithdrawalTransaction.builder()
                            .withdrawalId(saved.getWithdrawalId())
                            .status(TransactionStatus.PROCESSING.name())
                            .resultData(processingLog.toJSONString())
                            .build();

                    return withdrawalTransactionRepository.save(processingTran)
                            .then(recordWithdrawalEvent(saved.getWithdrawalId(), TransactionStatus.PROCESSING,
                                    "환급 처리 시작됨", null))
                            // ✅ 여기서 saved 를 다시 emit
                            .thenReturn(saved)
                            .doOnSuccess(v ->
                                    log.info("환급 처리 시작: withdrawalId={}, amount={}",
                                            saved.getWithdrawalId(), saved.getAmount()));
                })
                .flatMap(saved -> {

                    String payoutId = "TEST_PAYOUT_" + saved.getWithdrawalId() + "_" + System.currentTimeMillis();
                    saved.complete(payoutId);

                    return withdrawalRepository.save(saved)
                            .flatMap(updated -> {

                                JSONObject completedLog = new JSONObject();
                                completedLog.put("event", "환급 완료");
                                completedLog.put("status", "COMPLETED");
                                completedLog.put("payoutId", payoutId);
                                completedLog.put("originalAmount", updated.getAmount());
                                completedLog.put("timestamp", System.currentTimeMillis());

                                WithdrawalTransaction completedTran = WithdrawalTransaction.builder()
                                        .withdrawalId(updated.getWithdrawalId())
                                        .status(TransactionStatus.COMPLETED.name())
                                        .resultData(completedLog.toJSONString())
                                        .build();

                                return withdrawalTransactionRepository.save(completedTran)
                                        .then(recordWithdrawalEvent(updated.getWithdrawalId(), TransactionStatus.COMPLETED,
                                                "환급 완료", null))
                                        .then(paymentEventProducer.sendWithdrawalCompletedEvent(updated))
                                        .doOnSuccess(v2 ->
                                                log.info("✅ 환급 완료: withdrawalId={}, amount={}",
                                                        updated.getWithdrawalId(), updated.getAmount()));
                            });
                })
                .onErrorResume(e -> {

                    log.error("환급 처리 오류: withdrawalId={}, error={}",
                            wid, e.getMessage(), e);

                    return Mono.defer(() -> {
                                withdrawal.fail(e.getMessage());
                                return withdrawalRepository.save(withdrawal);
                            })
                            .flatMap(saved -> {

                                JSONObject failedLog = new JSONObject();
                                failedLog.put("event", "환급 실패");
                                failedLog.put("status", "FAILED");
                                failedLog.put("error", e.getMessage());
                                failedLog.put("timestamp", System.currentTimeMillis());

                                WithdrawalTransaction failedTran = WithdrawalTransaction.builder()
                                        .withdrawalId(saved.getWithdrawalId())
                                        .status(TransactionStatus.FAILED.name())
                                        .errorMessage(e.getMessage())
                                        .resultData(failedLog.toJSONString())
                                        .build();

                                return withdrawalTransactionRepository.save(failedTran)
                                        .then(recordWithdrawalEvent(saved.getWithdrawalId(), TransactionStatus.FAILED,
                                                "환급 실패", e.getMessage()));
                            })
                            .onErrorResume(ex -> {
                                log.error("환급 실패 기록 오류: {}", ex.getMessage());
                                return Mono.empty();
                            });
                })
                .then();
    }

    public Mono<WithdrawalResponseDto> getWithdrawal(Long userId, Long withdrawalId) {
        return withdrawalRepository.findById(withdrawalId)
                .switchIfEmpty(Mono.error(new CustomException(WITHDRAWAL_NOT_FOUND)))
                .flatMap(withdrawal -> {
                    if (!withdrawal.getUserId().equals(userId)) {
                        return Mono.error(new CustomException(UNAUTHORIZED_ACCESS));
                    }
                    return Mono.just(WithdrawalResponseDto.from(withdrawal));
                });
    }

    public Flux<WithdrawalResponseDto> getWithdrawalList(Long userId) {
        return withdrawalRepository.findByUserId(userId)
                .map(WithdrawalResponseDto::from);
    }

    // ===================== 이벤트 기록 =====================

    private Mono<Void> recordPaymentEvent(Payment payment, TransactionStatus status,
                                          String description, String errorMessage) {

        PaymentEvent event = PaymentEvent.builder()
                .paymentId(payment.getPaymentId())
                .status(status.name())
                .description(description)
                .errorMessage(errorMessage)
                .build();

        return paymentEventRepository.save(event)
                .doOnSuccess(ev ->
                        log.info("결제 이벤트 기록: paymentId={}, status={}, description={}",
                                payment.getPaymentId(), status, description))
                .then();
    }

    private Mono<Void> recordWithdrawalEvent(Long withdrawalId, TransactionStatus status,
                                             String description, String errorMessage) {

        log.info("환급 이벤트 기록: withdrawalId={}, status={}, description={}",
                withdrawalId, status, description);
        return Mono.empty();
    }
}
