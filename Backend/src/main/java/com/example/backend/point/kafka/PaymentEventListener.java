package com.example.backend.point.kafka;

import com.example.backend.global.exception.CustomException;
import com.example.backend.payment.entity.Payment;
import com.example.backend.payment.entity.WithdrawalRequest;
import com.example.backend.payment.enumType.TransactionStatus;
import com.example.backend.point.exception.PointErrorType;
import com.example.backend.point.service.PointService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 결제 WebFlux 서비스에서 발행한 Kafka 이벤트를 받아
 * 포인트 도메인(PointService) 로직을 실행하는 리스너.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentEventListener {

    private final PointService pointService;
    private final ObjectMapper objectMapper;

    /**
     * 결제 승인 완료 → 포인트 충전
     * topic: payment.confirmed
     */
    @KafkaListener(
            topics = "payment.confirmed",
            groupId = "point-mvc"
    )
    @Transactional
    public void handlePaymentConfirmed(String payload) throws JsonProcessingException {
        log.info("📥 결제 완료 이벤트 수신 (raw): {}", payload);

        // JSON → 백엔드 DTO 로 변환
        PaymentConfirmedEvent event =
                objectMapper.readValue(payload, PaymentConfirmedEvent.class);

        log.info("📥 결제 완료 이벤트 파싱: {}", event);

        try {
            Payment payment = Payment.builder()
                    .paymentId(event.getPaymentId())
                    .userId(event.getUserId())
                    .amount(event.getAmount())
                    .status(TransactionStatus.CONFIRMED)
                    .build();

            pointService.chargePoints(payment);

            log.info("✅ 결제 완료 포인트 충전 처리 성공: userId={}, paymentId={}, amount={}",
                    event.getUserId(), event.getPaymentId(), event.getAmount());

        } catch (CustomException e) {
            log.error("❌ 결제 포인트 처리 도메인 오류: event={}, errorType={}",
                    event, e.getErrorType(), e);
        } catch (Exception e) {
            log.error("❌ 결제 포인트 처리 시스템 오류: event={}, message={}",
                    event, e.getMessage(), e);
            throw e;
        }
    }

    /**
     * 환급 완료 → 포인트 차감
     * topic: withdrawal.completed
     */
    @KafkaListener(
            topics = "withdrawal.completed",
            groupId = "point-mvc"
    )
    @Transactional
    public void handleWithdrawalCompleted(String payload) throws JsonProcessingException {
        log.info("📥 환급 완료 이벤트 수신 (raw): {}", payload);

        WithdrawalCompletedEvent event =
                objectMapper.readValue(payload, WithdrawalCompletedEvent.class);

        log.info("📥 환급 완료 이벤트 파싱: {}", event);

        try {
            if (event.getAmount() == null || event.getAmount() <= 0) {
                throw new CustomException(PointErrorType.ERROR_INVALID_AMOUNT);
            }

            WithdrawalRequest withdrawal = WithdrawalRequest.builder()
                    .withdrawalId(event.getWithdrawalId())
                    .userId(event.getUserId())
                    .amount(event.getAmount())
                    .bankCode(event.getBankCode())
                    .status(TransactionStatus.COMPLETED)
                    .tossPayoutId(event.getPayoutId())
                    .completedAt(event.getCompletedAt())
                    .build();

            pointService.usePoints(withdrawal);

            log.info("✅ 환급 완료 포인트 차감 처리 성공: userId={}, withdrawalId={}, amount={}",
                    event.getUserId(), event.getWithdrawalId(), event.getAmount());

        } catch (CustomException e) {
            log.error("❌ 환급 포인트 처리 도메인 오류: event={}, errorType={}",
                    event, e.getErrorType(), e);
        } catch (Exception e) {
            log.error("❌ 환급 포인트 처리 시스템 오류: event={}, message={}",
                    event, e.getMessage(), e);
            throw e;
        }
    }
}
