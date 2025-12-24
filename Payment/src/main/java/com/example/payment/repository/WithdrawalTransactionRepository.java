package com.example.payment.repository;

import com.example.payment.entity.WithdrawalTransaction;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;

public interface WithdrawalTransactionRepository extends ReactiveCrudRepository<WithdrawalTransaction, Long> {
}
