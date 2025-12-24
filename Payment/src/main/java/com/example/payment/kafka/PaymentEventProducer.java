package com.example.payment.kafka;

import com.example.payment.entity.Payment;
import com.example.payment.entity.WithdrawalRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.concurrent.CompletableFuture;

@Component
@RequiredArgsConstructor
@Slf4j
public class PaymentEventProducer {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    private static final String PAYMENT_CONFIRMED_TOPIC    = "payment.confirmed";
    private static final String WITHDRAWAL_COMPLETED_TOPIC = "withdrawal.completed";

    // 결제 승인 이벤트
    public Mono<Void> sendPaymentConfirmedEvent(Payment payment) {

        PaymentConfirmedEvent event = PaymentConfirmedEvent.builder()
                .userId(payment.getUserId())
                .paymentId(payment.getPaymentId())
                .amount(payment.getAmount())
                // 필요하면 paymentKey, approvalAt 같은 필드도 이벤트에 추가해서 넘길 수 있음
                .createdAt(LocalDateTime.now())
                .build();

        CompletableFuture<SendResult<String, Object>> future =
                kafkaTemplate.send(PAYMENT_CONFIRMED_TOPIC,
                        String.valueOf(payment.getUserId()), event);       // [web:270][web:279]

        return Mono.fromFuture(future)                                     // [web:365][web:367]
                .doOnSuccess(result ->
                        log.info("✅ Kafka 전송 완료: topic={}, offset={}, event={}",
                                PAYMENT_CONFIRMED_TOPIC,
                                result.getRecordMetadata().offset(),
                                event))
                .doOnError(e ->
                        log.error("❌ Kafka 전송 실패: topic={}, error={}",
                                PAYMENT_CONFIRMED_TOPIC, e.getMessage(), e))
                .then();
    }

    // 환급 완료 이벤트
    public Mono<Void> sendWithdrawalCompletedEvent(WithdrawalRequest withdrawal) {

        WithdrawalCompletedEvent event = WithdrawalCompletedEvent.builder()
                .userId(withdrawal.getUserId())
                .withdrawalId(withdrawal.getWithdrawalId())
                .amount(withdrawal.getAmount())
                .bankCode(withdrawal.getBankCode())            // 새로 추가한 필드
                .payoutId(withdrawal.getTossPayoutId())        // 토스 지급 ID
                .completedAt(withdrawal.getCompletedAt())      // 완료 시각 (엔티티 값)
                .createdAt(LocalDateTime.now())                // 이벤트 생성 시각
                .build();

        CompletableFuture<SendResult<String, Object>> future =
                kafkaTemplate.send(WITHDRAWAL_COMPLETED_TOPIC,
                        String.valueOf(withdrawal.getUserId()), event);   // [web:270][web:279]

        return Mono.fromFuture(future)
                .doOnSuccess(result ->
                        log.info("✅ Kafka 전송 완료: topic={}, offset={}, event={}",
                                WITHDRAWAL_COMPLETED_TOPIC,
                                result.getRecordMetadata().offset(),
                                event))
                .doOnError(e ->
                        log.error("❌ Kafka 전송 실패: topic={}, error={}",
                                WITHDRAWAL_COMPLETED_TOPIC, e.getMessage(), e))
                .then();
    }
}
