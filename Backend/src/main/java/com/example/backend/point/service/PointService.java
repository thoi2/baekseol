package com.example.backend.point.service;

import com.example.backend.global.exception.CustomException;
import com.example.backend.point.dto.response.SurveyRefundPreviewResponse;
import com.example.backend.point.entity.PointRecord;
import com.example.backend.point.enumType.PointType;
import com.example.backend.point.enumType.ReferenceType;
import com.example.backend.point.exception.PointErrorType;
import com.example.backend.point.repository.PointRecordRepository;
import com.example.backend.survey.enumType.SurveyState;
import com.example.backend.user.entity.UserEntity;
import com.example.backend.user.repository.UserRepository;
import com.example.backend.survey.entity.Survey;
import com.example.backend.payment.entity.Payment;
import com.example.backend.payment.entity.WithdrawalRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

import static com.example.backend.point.exception.PointErrorType.*;

@Service
@Slf4j
@RequiredArgsConstructor
public class PointService {

    private final UserRepository userRepository;
    private final PointRecordRepository pointRecordRepository;

    private static final Long PAYMENT_FEE = 300L;
    private static final Long WITHDRAWAL_FEE = 300L;

    public Long getPaymentFee() { return PAYMENT_FEE; }
    public Long getWithdrawalFee() { return WITHDRAWAL_FEE; }

    private String generateContent(PointType type, ReferenceType refType, Survey survey, WithdrawalRequest withdrawal,
                                   Payment payment, String refundType) {
        switch(type) {
            case GET:
                switch(refType) {
                    case PAYMENT:
                        return "포인트 충전";
                    case REWARD:
                        return survey != null ? "설문 참여 - " + survey.getTitle() : "설문 참여 보상";
                    case REFUND:
                        String refundDesc = "";
                        if ("CANCELED".equalsIgnoreCase(refundType)) {
                            refundDesc = " 취소 환불";
                        } else if ("INSUFFICIENT".equalsIgnoreCase(refundType)) {
                            refundDesc = " 미달 환불";
                        }
                        return survey != null ? "설문 환불 - " + survey.getTitle() + refundDesc : "포인트 환불";
                    case ADMIN:
                        return "관리자 포인트 조정";
                    default:
                        return "포인트 획득";
                }
            case USE:
                switch(refType) {
                    case WITHDRAWAL:
                        if (withdrawal != null) {
                            String bankName = getBankName(withdrawal.getBankCode());
                            return "환급 신청 - " + bankName;
                        }
                        return "포인트 환급 신청";
                    case SURVEY:
                        return survey != null ? "설문 등록 - " + survey.getTitle() : "설문 등록";
                    case ADMIN:
                        return "관리자 포인트 조정";
                    default:
                        return "포인트 사용";
                }
            default:
                return "포인트 내역";
        }
    }

    private String getBankName(String code) {
        switch(code) {
            case "004": return "국민은행";
            case "011": return "농협은행";
            case "020": return "우리은행";
            case "088": return "신한은행";
            case "105": return "하나은행";
            case "090": return "카카오뱅크";
            case "098": return "토스뱅크";
            default: return code;
        }
    }

    @Transactional
    public void chargePoints(Payment payment) {
        UserEntity user = userRepository.findByIdForUpdate(payment.getUserId())
                .orElseThrow(() -> new CustomException(ERROR_USER_NOT_FOUND));

        Long amount = payment.getAmount()-PAYMENT_FEE;
        if (amount <= 0) throw new CustomException(ERROR_INVALID_AMOUNT);

        Long beforePoint = user.getPoint();
        user.chargePoint(amount);
        userRepository.save(user);

        String content = generateContent(PointType.GET, ReferenceType.PAYMENT, null, null, payment, null);

        PointRecord record = PointRecord.builder()
                .user(user)
                .amount(amount)
                .type(PointType.GET)
                .content(content)
                .remainPoint(user.getPoint())
                .referenceType(ReferenceType.PAYMENT)
                .referenceId(payment.getPaymentId())
                .platformFee(PAYMENT_FEE)
                .build();

        pointRecordRepository.save(record);

        log.info("포인트 충전: userId={}, amount={}, before={}, after={}, paymentId={}",
                user.getId(), amount, beforePoint, user.getPoint(), payment.getPaymentId());
    }

    @Transactional
    public void chargePointsForSurvey(Long userId, Survey survey) {

        UserEntity user = userRepository.findByIdForUpdate(userId)
                .orElseThrow(() -> new CustomException(ERROR_USER_NOT_FOUND));
        Long amount = survey.getReward();
        if (amount == null || amount <= 0) throw new CustomException(ERROR_INVALID_AMOUNT);

        Long beforePoint = user.getPoint();
        user.chargePoint(amount);
        userRepository.save(user);

        String content = generateContent(PointType.GET, ReferenceType.REWARD, survey, null, null, null);

        PointRecord record = PointRecord.builder()
                .user(user)
                .amount(amount)
                .type(PointType.GET)
                .content(content)
                .remainPoint(user.getPoint())
                .referenceType(ReferenceType.REWARD)
                .referenceId(survey.getSurveyId())
                .platformFee(null)
                .build();

        pointRecordRepository.save(record);

        log.info("설문 참여 보상: userId={}, amount={}, before={}, after={}, surveyId={}",
                user.getId(), amount, beforePoint, user.getPoint(), survey.getSurveyId());
    }
    // 환불 미리보기 (엔티티 버전)
    @Transactional(readOnly = true)
    public SurveyRefundPreviewResponse previewRefundSPointsForSurvey(UserEntity user, Survey survey) {
        // 1. 요청한 user가 해당 설문 등록자인지 확인
        if (!user.getId().equals(survey.getClient().getId())) {
            throw new CustomException(PointErrorType.ERROR_NOT_SURVEY_OWNER);
        }

        Long surveyId = survey.getSurveyId();

        // 2. ReferenceType.SURVEY, surveyId로 포인트 레코드 단건 조회
        PointRecord surveyRecord = pointRecordRepository
                .findByReferenceTypeAndReferenceId(ReferenceType.SURVEY, surveyId)
                .orElseThrow(() -> new CustomException(PointErrorType.ERROR_SURVEY_NO_REGISTRATION_POINTS));

        // 3. 총 결제 금액과 플랫폼 수수료 얻기
        long totalPaid = surveyRecord.getAmount();
        long platformFee = surveyRecord.getPlatformFee();

        // 4. 현재 참여 인원수 확인
        int participantCount = survey.getResponseCnt(); // 혹은 survey.getUserSurveys().size();

        // 5. 총 참여자 보상 금액 계산
        long totalRewardPaid = participantCount * survey.getReward();

        // 6. 환불 대상 금액 계산 (총 지불액 - 수수료 - 참여자 보상액)
        long rawRefundAmount = totalPaid - platformFee - totalRewardPaid;

        // 미리보기에서는 0 미만이면 0으로 보여주도록 처리 (원하면 그대로 둬도 됨)
        long refundAmount = Math.max(rawRefundAmount, 0L);

        // 7. 계산 결과를 DTO로 반환
        return new SurveyRefundPreviewResponse(
                surveyId,
                totalPaid,
                platformFee,
                participantCount,
                totalRewardPaid,
                refundAmount
        );
    }


    @Transactional
    public void refundSPointsForSurvey(Survey survey) {
        Long userId = survey.getClient().getId();  // 설문에 있는 등록자 ID 사용

        UserEntity user = userRepository.findByIdForUpdate(userId)
                .orElseThrow(() -> new CustomException(ERROR_USER_NOT_FOUND));

        Long surveyId = survey.getSurveyId();

        // 2. ReferenceType.SURVEY, surveyId로 포인트 레코드 단건 조회 (Optional 반환)
        PointRecord surveyRecord = pointRecordRepository
                .findByReferenceTypeAndReferenceId(ReferenceType.SURVEY, surveyId)
                .orElseThrow(() -> new CustomException(PointErrorType.ERROR_SURVEY_NO_REGISTRATION_POINTS));

        // 3. 총 결제 금액과 플랫폼 수수료 얻기
        long totalPaid = surveyRecord.getAmount();
        long platformFee = surveyRecord.getPlatformFee();

        // 4. 현재 참여 인원수 확인
        int participantCount = survey.getResponseCnt(); // 혹은 survey.getUserSurveys().size();

        // 5. 총 참여자 보상 금액 계산
        long totalRewardPaid = participantCount * survey.getReward();

        // 6. 환불 대상 금액 계산 (총 지불액 - 수수료 - 참여자 보상액)
        long refundAmount = totalPaid - platformFee - totalRewardPaid;

        if (refundAmount <= 0) {
            throw new CustomException(PointErrorType.ERROR_NO_REFUND_AVAILABLE);
        }

        // 7. 등록자에게 환불 진행
        Long beforePoint = user.getPoint();
        user.chargePoint(refundAmount);
        userRepository.save(user);

        // 8. 환불 이유에 따른 문자열 결정
        String refundType = null;
        if (survey.getState() == SurveyState.CANCELED) {
            refundType = "CANCELED";
        } else if (survey.getState() == SurveyState.DONE) {
            refundType = "INSUFFICIENT";
        } else {
            throw new CustomException(PointErrorType.ERROR_INVALID_SURVEY_STATE);
        }

        // 8-1. generateContent로 기본 메시지 생성
        String baseContent = generateContent(
                PointType.GET,
                ReferenceType.REFUND,
                survey,
                null,
                null,
                refundType
        );

        // 8-2. content에 수수료, 참여 인원, 총 환불액 정보 추가
        String content = String.format(
                "%s (수수료: %,dP, 참여 인원: %d명, 총 환불액: %,dP)",
                baseContent,
                platformFee,
                participantCount,
                refundAmount
        );

        // 9. 환불 기록 생성
        PointRecord refundRecord = PointRecord.builder()
                .user(user)
                .amount(refundAmount)
                .type(PointType.GET)
                .content(content)
                .remainPoint(user.getPoint())
                .referenceType(ReferenceType.REFUND)
                .referenceId(surveyId)
                .platformFee(null)
                .build();

        pointRecordRepository.save(refundRecord);

        log.info("설문 등록자 환불: userId={}, refundAmount={}, before={}, after={}, surveyId={}, refundType={}, platformFee={}, participantCount={}",
                user.getId(), refundAmount, beforePoint, user.getPoint(), surveyId, refundType, platformFee, participantCount);
    }




    @Transactional
    public void usePoints(WithdrawalRequest withdrawal) {
        UserEntity user = userRepository.findByIdForUpdate(withdrawal.getUserId())
                .orElseThrow(() -> new CustomException(ERROR_USER_NOT_FOUND));

        Long amount = withdrawal.getAmount();
        if (amount <= 0) throw new CustomException(ERROR_INVALID_AMOUNT);
        if (user.getPoint() < amount) throw new CustomException(ERROR_INSUFFICIENT_POINTS);

        Long beforePoint = user.getPoint();
        user.usePoint(amount);
        userRepository.save(user);

        String content = generateContent(PointType.USE, ReferenceType.WITHDRAWAL, null, withdrawal, null, null);

        PointRecord record = PointRecord.builder()
                .user(user)
                .amount(amount)
                .type(PointType.USE)
                .content(content)
                .remainPoint(user.getPoint())
                .referenceType(ReferenceType.WITHDRAWAL)
                .referenceId(withdrawal.getWithdrawalId())
                .platformFee(WITHDRAWAL_FEE)
                .build();

        pointRecordRepository.save(record);

        log.info("환급 신청: userId={}, amount={}, before={}, after={}, withdrawalId={}",
                user.getId(), amount, beforePoint, user.getPoint(), withdrawal.getWithdrawalId());
    }

    @Transactional
    public void usePointsForSurvey(Survey survey) {
        Long userId = survey.getClient().getId();  // 설문에 있는 등록자 ID 사용

        UserEntity user = userRepository.findByIdForUpdate(userId)
                .orElseThrow(() -> new CustomException(ERROR_USER_NOT_FOUND));
        Integer questionCnt = survey.getQuestionCnt();
        Integer maxResponse = survey.getMaxResponse();
        Long reward = survey.getReward();

        Long totalFee = calculatePriceForCreateSurvey(questionCnt, maxResponse, reward);
        Long userPayout = maxResponse * reward;
        Long platformFee = totalFee - userPayout;

        if (totalFee <= 0) throw new CustomException(ERROR_INVALID_AMOUNT);
        if (user.getPoint() < totalFee) throw new CustomException(ERROR_INSUFFICIENT_POINTS);

        Long beforePoint = user.getPoint();
        user.usePoint(totalFee);
        userRepository.save(user);

        String content = generateContent(PointType.USE, ReferenceType.SURVEY, survey, null, null, null);

        PointRecord record = PointRecord.builder()
                .user(user)
                .amount(totalFee)
                .type(PointType.USE)
                .content(content)
                .remainPoint(user.getPoint())
                .referenceType(ReferenceType.SURVEY)
                .referenceId(survey.getSurveyId())
                .platformFee(platformFee)
                .build();

        pointRecordRepository.save(record);

        log.info("설문 등록 포인트 차감: userId={}, totalFee={}, platformFee={}, before={}, after={}, surveyId={}",
                user.getId(), totalFee, platformFee, beforePoint, user.getPoint(), survey.getSurveyId());
    }

    @Transactional
    public void adminAdjustPoints(UserEntity user, Long amount, PointType type, String customReason) {
        if (amount == null || amount <= 0) throw new CustomException(ERROR_INVALID_AMOUNT);

        Long beforePoint = user.getPoint();

        if (type == PointType.GET) {
            user.chargePoint(amount);
        } else {
            if (user.getPoint() < amount) throw new CustomException(ERROR_INSUFFICIENT_POINTS);
            user.usePoint(amount);
        }
        userRepository.save(user);

        String content = "관리자 포인트 조정";
        if (customReason != null && !customReason.isEmpty()) {
            content += " (" + customReason + ")";
        }

        PointRecord record = PointRecord.builder()
                .user(user)
                .amount(amount)
                .type(type)
                .content(content)
                .remainPoint(user.getPoint())
                .referenceType(ReferenceType.ADMIN)
                .referenceId(null)
                .platformFee(null)
                .build();

        pointRecordRepository.save(record);

        log.info("관리자 포인트 조정: userId={}, amount={}, type={}, before={}, after={}",
                user.getId(), amount, type, beforePoint, user.getPoint());
    }

    public Long getUserPoints(Long userId) {
        UserEntity user = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(ERROR_USER_NOT_FOUND));
        return user.getPoint();
    }

    public List<PointRecord> getPointHistory(Long userId) {
        userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(ERROR_USER_NOT_FOUND));
        return pointRecordRepository.findByUserIdOrderByCreatedAtDesc(userId);
    }

    private Long calculatePriceForCreateSurvey(Integer questionCnt, Integer maxResponse, Long reward) {
        final int DEFAULT_PRICE_FOR_SURVEY = 3000;

        Double pointPerQuestion;
        if (questionCnt <= 10) pointPerQuestion = 1.0;
        else if (questionCnt <= 30) pointPerQuestion = 1.3;
        else if (questionCnt <= 60) pointPerQuestion = 1.6;
        else if (questionCnt <= 100) pointPerQuestion = 1.8;
        else pointPerQuestion = 2.0;

        Double pointPerResponse;
        if (maxResponse <= 10) pointPerResponse = 1.0;
        else if (maxResponse <= 100) pointPerResponse = 1.3;
        else if (maxResponse <= 500) pointPerResponse = 1.6;
        else if (maxResponse <= 1000) pointPerResponse = 1.8;
        else pointPerResponse = 2.0;

        return Math.round((maxResponse * reward) + (pointPerQuestion * pointPerResponse * DEFAULT_PRICE_FOR_SURVEY));
    }
}
