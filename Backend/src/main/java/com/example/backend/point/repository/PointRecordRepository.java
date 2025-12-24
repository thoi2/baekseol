package com.example.backend.point.repository;

import com.example.backend.point.entity.PointRecord;
import com.example.backend.point.enumType.PointType;
import com.example.backend.point.enumType.ReferenceType;
import com.example.backend.user.entity.UserEntity;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface PointRecordRepository extends JpaRepository<PointRecord, Long> {

    // 특정 사용자의 포인트 기록을 최신순으로 조회
    List<PointRecord> findByUserIdOrderByCreatedAtDesc(Long userId);

    // 특정 참조 타입과 참조 ID에 해당하는 포인트 기록 조회 (예: 특정 결제 내역)
    Optional<PointRecord> findByReferenceTypeAndReferenceId(ReferenceType referenceType, Long referenceId);

    // 특정 참조 타입과 기간 내 플랫폼 수수료 합계 계산
    @Query("SELECT COALESCE(SUM(pr.platformFee), 0) FROM PointRecord pr " +
            "WHERE pr.referenceType = :refType " +
            "AND pr.createdAt BETWEEN :start AND :end")
    Long sumPlatformFeeByTypeAndDateRange(@Param("refType") ReferenceType refType,
                                          @Param("start") LocalDateTime start,
                                          @Param("end") LocalDateTime end);

    // 모든 플랫폼 수수료 합계 조회
    @Query("SELECT COALESCE(SUM(pr.platformFee), 0) FROM PointRecord pr WHERE pr.platformFee IS NOT NULL")
    Long sumAllPlatformFees();

    // 특정 참조 타입과 기간 내 포인트 금액 합계 계산
    @Query("SELECT COALESCE(SUM(pr.amount), 0) FROM PointRecord pr " +
            "WHERE pr.referenceType = :refType " +
            "AND pr.createdAt BETWEEN :start AND :end")
    Long sumAmountByTypeAndDateRange(@Param("refType") ReferenceType refType,
                                     @Param("start") LocalDateTime start,
                                     @Param("end") LocalDateTime end);

    // 전체 잔여 포인트 합계 조회 (실시간 유통중 포인트 총합)
    @Query("SELECT COALESCE(SUM(pr.remainPoint), 0) FROM PointRecord pr")
    Long getCurrentPoints();

    // 오늘 활동한(포인트 입출 기록이 있는) 고유 사용자 수 집계
    @Query("SELECT COUNT(DISTINCT pr.user.id) FROM PointRecord pr WHERE DATE(pr.createdAt) = CURRENT_DATE")
    Long countActiveUsersToday();

    // 특정 타입별 지급/사용 포인트 합계 및 기간 내 합계
    @Query("SELECT COALESCE(SUM(pr.amount), 0) FROM PointRecord pr WHERE pr.type = :type AND pr.createdAt BETWEEN :start AND :end")
    Long sumAmountByTypeAndDateRange(@Param("type") PointType type,
                                     @Param("start") LocalDateTime start,
                                     @Param("end") LocalDateTime end);
    // 관리자 포인트 로그 검색
    @Query("""
        SELECT pr
        FROM PointRecord pr
        JOIN FETCH pr.user u
        WHERE (:userId IS NULL OR u.id = :userId)
          AND (:username IS NULL OR LOWER(u.username) LIKE LOWER(CONCAT('%', :username, '%')))
          AND (:referenceType IS NULL OR pr.referenceType = :referenceType)
        ORDER BY pr.createdAt DESC
    """)
    Page<PointRecord> searchPointLogs(
            @Param("userId") Long userId,
            @Param("username") String username,
            @Param("referenceType") ReferenceType referenceType,
            Pageable pageable
    );


}
