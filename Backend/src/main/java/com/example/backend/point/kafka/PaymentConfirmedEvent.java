package com.example.backend.point.kafka;

import lombok.Builder;
import lombok.Getter;
import lombok.ToString;

import java.time.LocalDateTime;

@Getter
@Builder
@ToString
public class PaymentConfirmedEvent {

    private final Long userId;
    private final Long paymentId;
    private final Long amount;
    private final LocalDateTime createdAt;   // 이벤트 생성 시각
}
