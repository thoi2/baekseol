package com.example.backend.point.kafka;

import lombok.Builder;
import lombok.Getter;
import lombok.ToString;

import java.time.LocalDateTime;

@Getter
@Builder
@ToString
public class WithdrawalCompletedEvent {

    private final Long userId;          // 환급한 사용자 ID
    private final Long withdrawalId;    // withdrawal_requests PK
    private final Long amount;          // 환급 요청 금액
    private final String bankCode;      // 은행 코드 (004, 090 등)
    private final String payoutId;      // 토스 지급 ID 또는 내부 지급 ID
    private final LocalDateTime completedAt; // 환급 완료 시각
    private final LocalDateTime createdAt;   // 이벤트 생성 시각 (옵션)
}
