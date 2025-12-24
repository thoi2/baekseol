package com.example.backend.survey.service;

import com.example.backend.global.enumType.AgeGroup;
import com.example.backend.global.exception.CustomException;
import com.example.backend.interest.entity.Interest;
import com.example.backend.interest.service.InterestService;
import com.example.backend.point.dto.response.SurveyRefundPreviewResponse;
import com.example.backend.point.service.PointService;
import com.example.backend.survey.dto.request.CreateSurveyReqDto;
import com.example.backend.survey.dto.request.SurveyParticipateReqDto;
import com.example.backend.survey.dto.request.SurveySearchReqDto;
import com.example.backend.survey.dto.response.*;
import com.example.backend.survey.entity.*;
import com.example.backend.survey.enumType.SortType;
import com.example.backend.survey.enumType.SurveyState;
import com.example.backend.survey.repository.AnswerRepository;
import com.example.backend.survey.repository.QuestionRepository;
import com.example.backend.survey.repository.SurveyRepository;
import com.example.backend.survey.repository.UserSurveyRepository;
import com.example.backend.user.entity.UserEntity;
import com.example.backend.user.repository.UserRepository;
import com.example.backend.user.service.UserService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import static com.example.backend.survey.exception.SurveyErrorType.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class SurveyService {

    private final UserService userService;
    private final SurveyStatisticsService surveyStatisticsService;
    private final InterestService interestService;
    private final PointService pointService;
    private final UserRepository userRepository;
    private final SurveyRepository surveyRepository;
    private final UserSurveyRepository userSurveyRepository;
    private final QuestionRepository questionRepository;
    private final AnswerRepository answerRepository;

    // 설문 생성
    @Transactional
    public SurveyResDto createSurvey(Long clientId, CreateSurveyReqDto requestForm) {
        UserEntity client = userRepository.findById(clientId)
                .orElseThrow(() -> new CustomException(ERROR_CREATE_SURVEY));
        Interest interest = interestService.getInterest(requestForm.interestId());

        Survey survey = Survey.builder()
                .client(client)
                .title(requestForm.title())
                .description(requestForm.description())
                .maxResponse(requestForm.maxResponse())
                .reward(requestForm.reward())
                .deadline(requestForm.deadline())
                .interest(interest)
                .state(SurveyState.IN_PROCESS)
                .responseCnt(0)
                .build();

        List<Question> questions = requestForm.questions().stream()
                .map(questionReqDto -> {
                    Question question = new Question();
                    question.setSurvey(survey);
                    question.setNumber(questionReqDto.number());
                    question.setContent(questionReqDto.content());
                    question.setType(questionReqDto.type());

                    List<Choice> choices = questionReqDto.choices().stream()
                            .map(choiceReqDto -> {
                                Choice choice = new Choice();
                                choice.setQuestion(question);
                                choice.setContent(choiceReqDto.content());
                                choice.setNumber(choiceReqDto.number());
                                return choice;
                            })
                            .collect(Collectors.toList());
                    question.setChoices(choices);
                    return question;
                })
                .collect(Collectors.toList());

        survey.setQuestions(questions);
        survey.setQuestionCnt(questions.size());


        Survey savedSurvey = surveyRepository.save(survey);
        // 포인트 결제
        pointService.usePointsForSurvey(survey);
        surveyStatisticsService.createParticipantStatistics(survey);

        return SurveyResDto.from(savedSurvey);
    }

    // 설문 전체 조회
    @Transactional
    public SurveyListResDto getAllSurvey() {
        List<Survey> surveys = surveyRepository.findAll();

        List<SurveyItemResDto> surveyItems = new ArrayList();
        for (Survey survey : surveys) {
            surveyItems.add(SurveyItemResDto.from(survey));
        }

        return SurveyListResDto.from(surveyItems);
    }

    // 단일 설문 조회 (참여)
    @Transactional
    public SurveyDetailResDto getSurveyBySurveyId(Long surveyId) {
        return SurveyDetailResDto.from(surveyRepository.findById(surveyId)
                .orElseThrow(() -> new CustomException(ERROR_GET_SURVEY)));
    }

    // 내가 의뢰한 설문 목록 조회
    @Transactional
    public SurveyListResDto getSurveyByClientId(Long clientId) {
        List<Survey> surveys = surveyRepository.findAllByClientIdOrderByCreatedAtDesc(clientId)
                .orElseThrow(() -> new CustomException(ERROR_GET_SURVEY_LIST));

        if (surveys.isEmpty()) throw new CustomException(ERROR_GET_SURVEY_LIST);

        List<SurveyItemResDto> surveyItems = new ArrayList();
        for (Survey survey : surveys) {
            surveyItems.add(SurveyItemResDto.from(survey));
        }

        return SurveyListResDto.from(surveyItems);
    }

    // 설문 검색하기
    @Transactional
    public Page<SurveyItemResDto> getSurveyByCondition(SurveySearchReqDto condition, Pageable pageable) {
        Pageable sortedPageable = createSortedPageable(condition.sortType(), pageable);

        Page<Survey> surveys = surveyRepository.findAllByCondition(condition.title(), condition.interestId(), SurveyState.IN_PROCESS, sortedPageable);

        return surveys.map(SurveyItemResDto::from);
    }

    // 정렬 기준에 따른 Pageable 반환
    private Pageable createSortedPageable(SortType sortType, Pageable pageable) {
        if (sortType == null) sortType = SortType.LATEST; // 기본 정렬 기준 최신순

        Sort sort = switch (sortType) {
            case LATEST -> Sort.by(Sort.Direction.DESC, "createdAt");
            case DEADLINE_NEAR -> Sort.by(Sort.Direction.ASC, "deadline");
            case POPULAR -> Sort.by(Sort.Direction.DESC, "responseCnt");
            case REWARD_HIGH -> Sort.by(Sort.Direction.DESC, "reward");
            default -> throw new CustomException(ERROR_GET_SURVEY_LIST);
        };

        return PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), sort);
    }

    // 설문 추천
    @Transactional
    public SurveyListResDto getSurveyByRecommend(Long userId) {
        List<UserSurvey> userSurveys = userSurveyRepository.findByUserIdWithInterestAndSurvey(userId);
        if (userSurveys.isEmpty()) return getTop10ByReward();

        List<Interest> topInterests = userSurveys.stream()
                .map(UserSurvey::getInterest)
                .collect(Collectors.groupingBy(Function.identity(), Collectors.counting()))
                .entrySet().stream()
                .sorted(Map.Entry.<Interest, Long>comparingByValue().reversed())
                .limit(10)
                .map(Map.Entry::getKey)
                .toList();

        Set<Long> participatedSurveyIds = userSurveys.stream()
                .map(us -> us.getSurvey().getSurveyId())
                .collect(Collectors.toSet());

        List<Survey> recommendedSurveys = surveyRepository.findByInterestInAndNotParticipated(topInterests, participatedSurveyIds);
        if (recommendedSurveys.isEmpty()) return getTop10ByReward();

        List<SurveyItemResDto> recommendedSurveyItems = new ArrayList<>();
        for (Survey survey : recommendedSurveys) {
            recommendedSurveyItems.add(SurveyItemResDto.from(survey));
        }

        return SurveyListResDto.from(recommendedSurveyItems);
    }

    // 내가 참여한 설문 조회
    @Transactional
    public Page<SurveyItemResDto> getSurveyByParticipantId(Long participantId, Pageable pageable) {
        Pageable sortedPageable = createSortedPageable(SortType.LATEST, pageable);
        Page<Survey> surveys = surveyRepository.findAllByParticipantId(participantId, sortedPageable);

        return surveys.map(SurveyItemResDto::from);
    }

    // 설문 참여
    @Transactional
    public void participateSurvey(Long participantId, Long surveyId, SurveyParticipateReqDto requestForm) {
        UserEntity participant = userRepository.findById(participantId)
                .orElseThrow(() -> new CustomException(ERROR_PARTICIPATE_SURVEY_USER_NOT_FOUND));

        if (userSurveyRepository.existsByUser_IdAndSurvey_SurveyId(participantId, surveyId))
            throw new CustomException(ERROR_PARTICIPATE_SURVEY_ALREADY_DONE);

        Survey survey = surveyRepository.findById(surveyId)
                .orElseThrow(() -> new CustomException(ERROR_PARTICIPATE_SURVEY_NO_CONTENT));

        if (survey.getState() != SurveyState.IN_PROCESS)
            throw new CustomException(ERROR_PARTICIPATE_SURVEY_CLOSED);

        List<Answer> answers = requestForm.answers().stream()
                .map(answerDto -> Answer.builder()
                        .survey(survey)
                        .participant(participant)
                        .questionNumber(answerDto.number())
                        .type(answerDto.questionType())
                        .answerChoice(answerDto.answerChoices())
                        .content(answerDto.content())
                        .build()
                ).toList();
        answerRepository.saveAll(answers);

        UserSurvey userSurvey = UserSurvey.builder()
                .user(participant)
                .survey(survey)
                .age(participant.getAge())
                .ageGroup(AgeGroup.fromAge(participant.getAge()))
                .gender(participant.getGender())
                .workType(participant.getWorkType())
                .interest(survey.getInterest())
                .build();
        userSurveyRepository.save(userSurvey);

        // 설문 참여자 수 + 1
        survey.setResponseCnt(survey.getResponseCnt() + 1);
        if (survey.getResponseCnt().equals(survey.getMaxResponse())) survey.setState(SurveyState.DONE);

        surveyStatisticsService.updateParticipantStatistics(survey, userSurvey);

        pointService.chargePointsForSurvey(participant.getId(), survey);
    }

    // 질문 목록 조회 (설문 참여)
    public List<QuestionResDto> getQuestionsBySurveyId(Long surveyId) {
        Survey survey = surveyRepository.findById(surveyId)
                .orElseThrow(() -> new CustomException(ERROR_GET_SURVEY));

        List<Question> questions = questionRepository.findBySurvey_SurveyIdWithChoices(surveyId);
        List<QuestionResDto> questionResDtos = new ArrayList<>();
        for (Question question : questions) {
            questionResDtos.add(QuestionResDto.from(question));
        }

        return questionResDtos;
    }

    // 설문 환불 미리보기
    @Transactional
    public SurveyRefundPreviewResponse getSurveyRefundPreview(Long clientId, Long surveyId) {
        Survey survey = surveyRepository.findById(surveyId)
                .orElseThrow(() -> new CustomException(ERROR_GET_SURVEY));

        UserEntity client = survey.getClient();

        if (!clientId.equals(client.getId())) {
            throw new CustomException(ERROR_CLOSE_SURVEY_NOT_PERMISSION);
        }

        return pointService.previewRefundSPointsForSurvey(client, survey);
    }

    // 설문 내리기
    @Transactional
    public void closeSurveyBySurveyId(Long clientId, Long surveyId) {
        Survey target = surveyRepository.findById(surveyId)
                .orElseThrow(() -> new CustomException(ERROR_GET_SURVEY));
        UserEntity targetClient = target.getClient();

        if (!clientId.equals(targetClient.getId())) throw new CustomException(ERROR_CLOSE_SURVEY_NOT_PERMISSION);
        if (target.getState() != SurveyState.IN_PROCESS) throw new CustomException(ERROR_CLOSE_SURVEY_ALREADY_DONE);
        target.setState(SurveyState.CANCELED);
        pointService.refundSPointsForSurvey(target);
    }


    // 마감 임박 설문 10개 조회
    @Transactional
    public SurveyListResDto getTop10ByDeadline() {
        Pageable topTen = PageRequest.of(0, 10);
        List<Survey> surveys = surveyRepository.findTop10ByDeadlineImminent(SurveyState.IN_PROCESS, topTen);

        List<SurveyItemResDto> surveyItems = surveys.stream()
                .map(SurveyItemResDto::from)
                .toList();

        return SurveyListResDto.from(surveyItems);
    }

    // 포인트 높은 설문 10개 조회
    @Transactional
    public SurveyListResDto getTop10ByReward() {
        Pageable topTen = PageRequest.of(0, 10);
        List<Survey> surveys = surveyRepository.findTop10ByRewardImminent(SurveyState.IN_PROCESS, topTen);

        List<SurveyItemResDto> surveyItems = surveys.stream()
                .map(SurveyItemResDto::from)
                .toList();

        return SurveyListResDto.from(surveyItems);
    }

    // 참여자 높은 설문 10개 조회
    @Transactional
    public SurveyListResDto getTop10ByResponseCnt() {
        Pageable topTen = PageRequest.of(0, 10);
        List<Survey> surveys = surveyRepository.findTop10ByResponseCntImminent(SurveyState.IN_PROCESS, topTen);

        List<SurveyItemResDto> surveyItems = surveys.stream()
                .map(SurveyItemResDto::from)
                .toList();

        return SurveyListResDto.from(surveyItems);
    }

    // 설문 참여 여부 조회
    @Transactional
    public boolean getParticipatedSurvey(Long userId, Long surveyId) {
        return userSurveyRepository.existsByUser_IdAndSurvey_SurveyId(userId, surveyId);
    }
}
