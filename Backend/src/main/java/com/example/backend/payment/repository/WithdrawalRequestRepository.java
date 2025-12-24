package com.example.backend.payment.repository;

import com.example.backend.payment.entity.WithdrawalRequest;
import com.example.backend.payment.enumType.TransactionStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface WithdrawalRequestRepository 
        extends JpaRepository<WithdrawalRequest, Long> {
    List<WithdrawalRequest> findByUserId(Long userId);
//    List<WithdrawalRequest> findByStatus(TransactionStatus status);
//    Optional<WithdrawalRequest> findByTossPayoutId(String tossPayoutId);
    // 환급금액 합계(전체·오늘) 조회
    @Query("SELECT COALESCE(SUM(wr.amount), 0) FROM WithdrawalRequest wr WHERE wr.createdAt BETWEEN :start AND :end")
    Long sumAmountByDateRange(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    // 오늘 환급 건수 집계
    @Query("SELECT COUNT(wr) FROM WithdrawalRequest wr WHERE DATE(wr.createdAt) = CURRENT_DATE")
    Long countTodayWithdrawal();

    // 상태별 환급 건수 집계(대기중, 실패 등)
    @Query("SELECT COUNT(wr) FROM WithdrawalRequest wr WHERE wr.status = :status")
    Long countByStatus(@Param("status") TransactionStatus status);

}