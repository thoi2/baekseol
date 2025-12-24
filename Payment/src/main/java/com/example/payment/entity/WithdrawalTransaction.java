package com.example.payment.entity;

import com.example.payment.global.common.BaseTimeEntity;
import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;
import org.springframework.data.relational.core.mapping.Column;

@Table("withdrawal_transactions")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WithdrawalTransaction extends BaseTimeEntity {

    @Id
    private Long withdrawalTransactionId;

    @Column("withdrawal_id")
    private Long withdrawalId;  // 관계 매핑 대신 FK 컬럼만 직접 관리

    @Column("status")
    private String status;

    @Column("result_data")
    private String resultData;

    @Column("error_message")
    private String errorMessage;
}
