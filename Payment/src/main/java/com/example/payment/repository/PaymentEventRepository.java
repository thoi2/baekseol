package com.example.payment.repository;

import com.example.payment.entity.PaymentEvent;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;

public interface PaymentEventRepository extends ReactiveCrudRepository<PaymentEvent, Long> {
    // 필요 시 paymentId 기반 조회 메서드는 나중에 추가
}
