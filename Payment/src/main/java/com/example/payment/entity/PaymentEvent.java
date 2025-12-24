package com.example.payment.entity;

import com.example.payment.global.common.BaseTimeEntity;
import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;
import org.springframework.data.relational.core.mapping.Column;

@Table("payment_events")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PaymentEvent extends BaseTimeEntity {

    @Id
    private Long eventId;

    // ManyToOne 대신 FK 컬럼으로 직접 관리
    @Column("payment_id")
    private Long paymentId;

    // Enum 타입 문자열로 저장, 필요시 컨버터 적용 가능
    @Column("event_type")
    private String status;

    @Column("event_data")
    private String eventData;

    @Column("description")
    private String description;

    @Column("error_message")
    private String errorMessage;
}
