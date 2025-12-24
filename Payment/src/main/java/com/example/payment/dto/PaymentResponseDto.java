package com.example.payment.dto;

import com.example.payment.entity.Payment;
import com.example.payment.enumType.TransactionStatus;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class PaymentResponseDto {
    private Long paymentId;
    private Long amount;
    private String orderId;
    private String orderName;
    private String method;
    private TransactionStatus status;
    private String receiptUrl;
    
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime approvalAt;
    
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createdAt;

    // ✅ Entity → DTO 변환 (민감한 정보 제외)
    public static PaymentResponseDto from(Payment payment) {
        return PaymentResponseDto.builder()
                .paymentId(payment.getPaymentId())
                .amount(payment.getAmount())
                .orderId(payment.getOrderId())
                .orderName(payment.getTossOrderName())
                .method(payment.getMethod())
                .status(TransactionStatus.valueOf(payment.getStatus()))
                .receiptUrl(payment.getReceiptUrl())
                .approvalAt(payment.getApprovalAt())
                .createdAt(payment.getCreatedAt())
                // ✅ 제외: userId, paymentKey, internalId 등
                .build();
    }
}
