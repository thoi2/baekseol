package com.example.payment.repository;

import com.example.payment.entity.WithdrawalRequest;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;

public interface WithdrawalRequestRepository extends ReactiveCrudRepository<WithdrawalRequest, Long> {

    Flux<WithdrawalRequest> findByUserId(Long userId);          // 사용자 환급 목록
}
