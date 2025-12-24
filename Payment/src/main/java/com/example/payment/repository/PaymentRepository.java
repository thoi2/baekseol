package com.example.payment.repository;

import com.example.payment.entity.Payment;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface PaymentRepository extends ReactiveCrudRepository<Payment, Long> {

    Mono<Payment> findByPaymentKey(String paymentKey);          // 결제키로 단건 조회

    Flux<Payment> findByUserIdOrderByCreatedAtDesc(Long userId); // 사용자 결제 목록 최신순
}
