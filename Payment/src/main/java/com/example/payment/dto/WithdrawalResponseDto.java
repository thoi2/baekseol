package com.example.payment.dto;

import com.example.payment.entity.WithdrawalRequest;
import com.example.payment.enumType.TransactionStatus;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class WithdrawalResponseDto {
    private Long withdrawalId;
    private Long amount;
    private String bankCode;
    
    // ✅ 계좌번호 일부만 표시 (예: 1234****5678)
    private String maskedAccount;
    
    private TransactionStatus status;
    
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime requestedAt;
    
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime completedAt;

    // ✅ Entity → DTO 변환 (민감한 정보 제외)
    public static WithdrawalResponseDto from(WithdrawalRequest withdrawal) {
        return WithdrawalResponseDto.builder()
                .withdrawalId(withdrawal.getWithdrawalId())
                .amount(withdrawal.getAmount())
                .bankCode(withdrawal.getBankCode())
                .maskedAccount(maskAccount(withdrawal.getAccount()))  // ✅ 계좌 마스킹
                .status(TransactionStatus.valueOf(withdrawal.getStatus()))
                .requestedAt(withdrawal.getCreatedAt())
                .completedAt(withdrawal.getCompletedAt())
                // ✅ 제외: userId, fullAccount 등
                .build();
    }

    // ✅ 계좌번호 마스킹 (예: 1234567890 → 1234****90)
    private static String maskAccount(String account) {
        if (account == null || account.length() < 4) {
            return account;
        }
        int length = account.length();
        String first = account.substring(0, 4);
        String last = account.substring(length - 2);
        return first + "****" + last;
    }
}
