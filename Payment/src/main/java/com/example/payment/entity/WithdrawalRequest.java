package com.example.payment.entity;

import com.example.payment.global.common.BaseTimeEntity;
import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;
import org.springframework.data.relational.core.mapping.Column;
import java.time.LocalDateTime;

@Table("withdrawal_requests")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WithdrawalRequest extends BaseTimeEntity {

    @Id
    private Long withdrawalId;

    @Column("user_id")
    private Long userId;

    @Column("amount")
    private Long amount;

    @Column("bank_code")
    private String bankCode;

    @Column("account")
    private String account;

    @Column("status")
    private String status;

    @Column("toss_payout_id")
    private String tossPayoutId;

    @Column("processed_at")
    private LocalDateTime processedAt;

    @Column("completed_at")
    private LocalDateTime completedAt;

    @Column("failure_reason")
    private String failureReason;

    public void startProcessing() {
        if (!"PENDING".equals(status)) {
            throw new IllegalStateException("PENDING 상태에서만 처리 시작 가능");
        }
        this.status = "PROCESSING";
        this.processedAt = LocalDateTime.now();
    }

    public void complete(String payoutId) {
        if (!"PROCESSING".equals(status)) {
            throw new IllegalStateException("PROCESSING 상태에서만 완료 처리 가능");
        }
        this.status = "COMPLETED";
        this.tossPayoutId = payoutId;
        this.completedAt = LocalDateTime.now();
    }

    public void fail(String reason) {
        if (!"PROCESSING".equals(status)) {
            throw new IllegalStateException("PROCESSING 상태에서만 실패 처리 가능");
        }
        this.status = "FAILED";
        this.failureReason = reason;
    }
}
