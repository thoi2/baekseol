package com.example.backend.point.entity;

import com.example.backend.global.common.BaseTimeEntity;
import com.example.backend.point.enumType.PointType;
import com.example.backend.point.enumType.ReferenceType;
import com.example.backend.user.entity.UserEntity;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(
        name = "point_record",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_point_user_ref",
                        columnNames = {"user_id", "reference_type", "reference_id"}
                )
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PointRecord extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "point_record_id")
    private Long pointRecordId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private UserEntity user;

    @Column(name = "amount", nullable = false)
    private Long amount;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false,
            columnDefinition = "VARCHAR(20) DEFAULT 'GET'")
    private PointType type;

    @Column(name = "content", length = 255)
    private String content;

    @Column(name = "remain_point")
    private Long remainPoint;

    @Enumerated(EnumType.STRING)
    @Column(name = "reference_type", length = 20)
    private ReferenceType referenceType;

    @Column(name = "reference_id")
    private Long referenceId;

    // ✅ 범용 수수료 컬럼
    // - PAYMENT: 결제 수수료 (PG사 수수료)
    // - SURVEY: 플랫폼 수수료 (설문 등록 수수료)
    // - WITHDRAWAL: 송금 수수료 (은행 이체 수수료)
    // - 기타: null
    @Column(name = "platform_fee")
    private Long platformFee;
}
