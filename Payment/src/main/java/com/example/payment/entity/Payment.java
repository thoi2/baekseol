package com.example.payment.entity;

import com.example.payment.global.common.BaseTimeEntity;
import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;
import org.springframework.data.relational.core.mapping.Column;
import java.time.LocalDateTime;

@Table("payments")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Payment extends BaseTimeEntity {

    @Id
    private Long paymentId;

    @Column("user_id")
    private Long userId;

    @Column("payment_key")
    private String paymentKey;

    @Column("order_id")
    private String orderId;

    @Column("amount")
    private Long amount;

    @Column("status")
    private String status; // Enum은 String 변환하거나 컨버터 사용 필요

    @Column("method")
    private String method;

    @Column("toss_order_name")
    private String tossOrderName;

    @Column("toss_approval_at")
    private LocalDateTime tossApprovalAt;

    @Column("receipt_url")
    private String receiptUrl;

    @Column("approval_at")
    private LocalDateTime approvalAt;

    // 상태 변경 메서드 등은 기존과 동일하게 유지
    public void startProcessing(String paymentKey) {
        if (!"PENDING".equals(status)) {
            throw new IllegalStateException("PENDING 상태에서만 처리 시작 가능");
        }
        this.status = "PROCESSING";
        this.paymentKey = paymentKey;
    }

    public void confirmPayment(LocalDateTime approvalAt,
                               LocalDateTime tossApprovalAt,
                               String receiptUrl,
                               String method) {
        if (!"PROCESSING".equals(status)) {
            throw new IllegalStateException("PROCESSING 상태에서만 승인 가능");
        }
        this.status = "CONFIRMED";
        this.approvalAt = approvalAt;
        this.tossApprovalAt = tossApprovalAt;
        this.receiptUrl = receiptUrl;
        this.method = method;
    }

    public void failPayment() {
        if (!"PROCESSING".equals(status)) {
            throw new IllegalStateException("PROCESSING 상태에서만 실패 처리 가능");
        }
        this.status = "FAILED";
    }

    public void cancelPayment() {
        if (!"CONFIRMED".equals(status)) {
            throw new IllegalStateException("CONFIRMED 상태에서만 취소 가능");
        }
        this.status = "CANCELLED";
    }

    public void resetForRetry() {
        if (!"FAILED".equals(status)) {
            throw new IllegalStateException("FAILED 상태에서만 재시도 가능");
        }
        this.status = "PENDING";
        this.paymentKey = null;
        this.method = null;
        this.receiptUrl = null;
        this.approvalAt = null;
        this.tossApprovalAt = null;
    }
}
