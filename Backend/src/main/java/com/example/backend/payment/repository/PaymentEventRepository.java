package com.example.backend.payment.repository;

import com.example.backend.payment.entity.PaymentEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PaymentEventRepository extends JpaRepository<PaymentEvent, Long> {
//    List<PaymentEvent> findByPayment_PaymentId(Long paymentId);
//    List<PaymentEvent> findByPayment_PaymentIdOrderByCreatedAtDesc(Long paymentId);
}