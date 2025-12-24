package com.example.backend.payment.repository;

import com.example.backend.payment.entity.Payment;
import com.example.backend.payment.enumType.TransactionStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import java.util.Optional;
import java.util.List;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, Long> {

//    // paymentId로 검색 (기본 제공되지만 명시적으로 작성 가능)
//    Optional<Payment> findByPaymentId(Long paymentId);

    // paymentKey로 검색 (중복 결제 방지)
    Optional<Payment> findByPaymentKey(String paymentKey);

//    // paymentKey 존재 여부 확인 (중복 결제 방지)
//    boolean existsByPaymentKey(String paymentKey);
//
//    // orderId로 검색 (중복 결제 방지)
//    Optional<Payment> findByOrderId(String orderId);
//
//    // 사용자의 결제 목록
//    List<Payment> findByUserId(Long userId);
//
//    // 상태별 조회
//    List<Payment> findByStatus(TransactionStatus status);

    // 사용자 결제 목록 (최신순)
    List<Payment> findByUserIdOrderByCreatedAtDesc(Long userId);

    // 오늘 결제 건수 집계
    @Query("SELECT COUNT(p) FROM Payment p WHERE DATE(p.createdAt) = CURRENT_DATE")
    Long countTodayPayment();
}
