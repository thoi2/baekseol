package com.example.payment.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WithdrawalRequestDto {
    private Long amount;
    private String bankCode;
    private String account;
    private String password;
}
