package com.example.payment.global.common;

import lombok.Getter;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.relational.core.mapping.Column;

import java.time.LocalDateTime;

@Getter
public abstract class BaseTimeEntity {

    @CreatedDate
    @Column("created_at")
    private LocalDateTime createdAt;        // 생성 시각

    @LastModifiedDate
    @Column("updated_at")
    private LocalDateTime updatedAt;        // 수정 시각
}
