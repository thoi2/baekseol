package com.example.backend.global.data;

import com.example.backend.global.enumType.WorkType;
import com.example.backend.interest.entity.Interest;
import com.example.backend.interest.service.InterestService;
import com.example.backend.point.service.PointService;
import com.example.backend.survey.dto.request.AnswerReqDto;
import com.example.backend.survey.dto.request.ChoiceReqDto;
import com.example.backend.survey.dto.request.QuestionReqDto;
import com.example.backend.survey.dto.request.SurveyParticipateReqDto;
import com.example.backend.survey.entity.Choice;
import com.example.backend.survey.entity.Question;
import com.example.backend.survey.entity.Survey;
import com.example.backend.survey.enumType.QuestionType;
import com.example.backend.survey.enumType.SurveyState;
import com.example.backend.survey.repository.SurveyRepository;
import com.example.backend.survey.service.SurveyService;
import com.example.backend.survey.service.SurveyStatisticsService;
import com.example.backend.user.entity.UserEntity;
import com.example.backend.user.enumType.Gender;
import com.example.backend.user.enumType.UserRole;
import com.example.backend.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/data")
public class DataController {

    private final PasswordEncoder encoder;
    private final UserRepository userRepository;
    private final InterestService interestService;
    private final SurveyService surveyService;
    private final SurveyRepository surveyRepository;
    private final PointService pointService;
    private final SurveyStatisticsService surveyStatisticsService;

    private final Random random = new Random();

    // 1000개 Dummy user 생성
    @PostMapping("/create/user")
    public ResponseEntity<?> createDummyUsers() {

        WorkType[] workTypes = WorkType.values();

        List<UserEntity> users = new ArrayList<>();

        UserEntity user1 = UserEntity.create(
                "baekseol",
                "baekseol11@naver.com",
                encoder.encode("1234qwer!"),
                25L,
                random.nextInt(2) == 0 ? Gender.MALE : Gender.FEMALE,
                workTypes[random.nextInt(workTypes.length)]
        );
        user1.chargePoint(10000000L);
        user1.setUserRole(UserRole.ADMIN);
        users.add(user1);

        for (int i = 2; i < 1002; ++i) {
            users.add(UserEntity.create(
                    "user" + i,
                    "user" + i + "@test.com",
                    encoder.encode("1234qwer!"),
                    random.nextLong(5, 50) + 10,
                    random.nextInt(2) == 0 ? Gender.MALE : Gender.FEMALE,
                    workTypes[random.nextInt(workTypes.length)]
            ));
        }
        userRepository.saveAll(users);
        return ResponseEntity.ok().build();
    }

    // 5개 설문조사 생성
    @PostMapping("/create/survey")
    public ResponseEntity<?> createDummySurveys() {

        // 설문 1
        UserEntity client = userRepository.findByUsername("baekseol");
        Interest interest = interestService.getInterest(random.nextLong(5) + 1);
        Survey survey = Survey.builder()
                .client(client)
                .title("20대 자취생 소비 습관 설문")
                .description("20대 자취생의 소비 패턴과 절약 니즈를 파악하기 위한 설문입니다.")
                .maxResponse(1000)
                .reward(100L)
                .deadline(LocalDateTime.of(2025, random.nextInt(2) + 11, random.nextInt(1, 31), 23, 59))
                .interest(interest)
                .state(SurveyState.IN_PROCESS)
                .responseCnt(0)
                .build();

        List<QuestionReqDto> questionReqDtos = List.of(
                new QuestionReqDto(
                        1,
                        "현재 자취 기간은 얼마나 되나요?",
                        QuestionType.SINGLE_CHOICE,
                        List.of(
                                new ChoiceReqDto(1, "6개월 미만"),
                                new ChoiceReqDto(2, "6개월 ~ 1년"),
                                new ChoiceReqDto(3, "1년 ~ 3년"),
                                new ChoiceReqDto(4, "3년 이상")
                        )
                ),
                new QuestionReqDto(
                        2,
                        "한 달 평균 생활비(식비+공과금+기타)는 어느 정도인가요?",
                        QuestionType.SINGLE_CHOICE,
                        List.of(
                                new ChoiceReqDto(1, "50만원 미만"),
                                new ChoiceReqDto(2, "50만 ~ 80만원"),
                                new ChoiceReqDto(3, "80만 ~ 100만원"),
                                new ChoiceReqDto(4, "100만원 이상")
                        )
                ),
                new QuestionReqDto(
                        3,
                        "생활비를 관리할 때 가장 많이 사용하는 방법은 무엇인가요?",
                        QuestionType.SINGLE_CHOICE,
                        List.of(
                                new ChoiceReqDto(1, "가계부 앱 사용"),
                                new ChoiceReqDto(2, "엑셀·노션 등 직접 기록"),
                                new ChoiceReqDto(3, "머릿속으로 대략 관리"),
                                new ChoiceReqDto(4, "거의 관리하지 않는다")
                        )
                ),
                new QuestionReqDto(
                        4,
                        "최근 3개월 동안 가장 지출이 많았던 카테고리는 무엇인가요?",
                        QuestionType.SINGLE_CHOICE,
                        List.of(
                                new ChoiceReqDto(1, "식비/배달"),
                                new ChoiceReqDto(2, "여가/취미"),
                                new ChoiceReqDto(3, "쇼핑/패션"),
                                new ChoiceReqDto(4, "주거/관리비"),
                                new ChoiceReqDto(5, "기타")
                        )
                ),
                new QuestionReqDto(
                        5,
                        "본인이 생각하는 가장 크게 줄이고 싶은 지출 항목과 이유를 자유롭게 작성해 주세요.",
                        QuestionType.SUBJECTIVE,
                        List.of()
                )
        );

        makeSurvey(questionReqDtos, survey, client);

        // 설문 2
        Interest interest2 = interestService.getInterest(random.nextLong(5));

        Survey survey2 = Survey.builder()
                .client(client)
                .title("대학생 학습 및 시험 준비 습관 설문")
                .description("대학생들의 시험 대비 방식과 학습 습관을 파악하기 위한 설문입니다.")
                .maxResponse(800)
                .reward(700L)
                .deadline(LocalDateTime.of(2025, random.nextInt(2) + 11, random.nextInt(21, 31), 23, 59))
                .interest(interest2)
                .state(SurveyState.IN_PROCESS)
                .responseCnt(0)
                .build();

        List<QuestionReqDto> questionReqDtos2 = List.of(
                new QuestionReqDto(
                        1,
                        "시험 공부는 보통 언제부터 시작하나요?",
                        QuestionType.SINGLE_CHOICE,
                        List.of(
                                new ChoiceReqDto(1, "시험 일주일 전부터"),
                                new ChoiceReqDto(2, "시험 2주 전부터"),
                                new ChoiceReqDto(3, "시험 한 달 전부터"),
                                new ChoiceReqDto(4, "시험 직전에 몰아서")
                        )
                ),
                new QuestionReqDto(
                        2,
                        "가장 선호하는 학습 방식은 무엇인가요?",
                        QuestionType.SINGLE_CHOICE,
                        List.of(
                                new ChoiceReqDto(1, "강의 위주 수업 듣기"),
                                new ChoiceReqDto(2, "혼자 정리하며 공부"),
                                new ChoiceReqDto(3, "스터디 모임 참여"),
                                new ChoiceReqDto(4, "온라인 강의·요약 영상 활용")
                        )
                ),
                new QuestionReqDto(
                        3,
                        "시험 기간 동안 하루 평균 공부 시간은 어느 정도인가요?",
                        QuestionType.SINGLE_CHOICE,
                        List.of(
                                new ChoiceReqDto(1, "2시간 미만"),
                                new ChoiceReqDto(2, "2~4시간"),
                                new ChoiceReqDto(3, "4~6시간"),
                                new ChoiceReqDto(4, "6시간 이상")
                        )
                ),
                new QuestionReqDto(
                        4,
                        "시험 대비에 가장 도움이 된다고 느끼는 도구는 무엇인가요?",
                        QuestionType.SINGLE_CHOICE,
                        List.of(
                                new ChoiceReqDto(1, "교수님 강의자료"),
                                new ChoiceReqDto(2, "교과서·참고서"),
                                new ChoiceReqDto(3, "노트·필기 정리"),
                                new ChoiceReqDto(4, "요약본/족보/기출문제")
                        )
                ),
                new QuestionReqDto(
                        5,
                        "본인만의 공부 노하우나 루틴이 있다면 자유롭게 작성해 주세요.",
                        QuestionType.SUBJECTIVE,
                        List.of()
                )
        );

        makeSurvey(questionReqDtos2, survey2, client);

        // 설문 3
        Interest interest3 = interestService.getInterest(random.nextLong(5));

        Survey survey3 = Survey.builder()
                .client(client)
                .title("MZ세대 배달앱 및 외식 소비 설문")
                .description("배달앱과 외식 빈도, 선택 기준을 파악하기 위한 설문입니다.")
                .maxResponse(1200)
                .reward(600L)
                .deadline(LocalDateTime.of(2025, random.nextInt(2) + 11, random.nextInt(21, 31), 23, 59))
                .interest(interest3)
                .state(SurveyState.IN_PROCESS)
                .responseCnt(0)
                .build();

        List<QuestionReqDto> questionReqDtos3 = List.of(
                new QuestionReqDto(
                        1,
                        "일주일에 배달 음식을 몇 번 정도 이용하시나요?",
                        QuestionType.SINGLE_CHOICE,
                        List.of(
                                new ChoiceReqDto(1, "거의 이용하지 않는다"),
                                new ChoiceReqDto(2, "1~2회"),
                                new ChoiceReqDto(3, "3~4회"),
                                new ChoiceReqDto(4, "5회 이상")
                        )
                ),
                new QuestionReqDto(
                        2,
                        "배달앱을 선택할 때 가장 중요한 기준은 무엇인가요?",
                        QuestionType.SINGLE_CHOICE,
                        List.of(
                                new ChoiceReqDto(1, "배달비"),
                                new ChoiceReqDto(2, "할인·쿠폰"),
                                new ChoiceReqDto(3, "리뷰·평점"),
                                new ChoiceReqDto(4, "배송 속도")
                        )
                ),
                new QuestionReqDto(
                        3,
                        "최근 3개월간 배달비 인상으로 인해 이용 패턴에 변화가 있었나요?",
                        QuestionType.SINGLE_CHOICE,
                        List.of(
                                new ChoiceReqDto(1, "이전보다 자주 시킨다"),
                                new ChoiceReqDto(2, "이전과 비슷하다"),
                                new ChoiceReqDto(3, "이전보다 줄였다"),
                                new ChoiceReqDto(4, "배달 대신 직접 포장·방문한다")
                        )
                ),
                new QuestionReqDto(
                        4,
                        "외식/배달 지출에서 가장 많이 이용하는 음식 카테고리는 무엇인가요?",
                        QuestionType.SINGLE_CHOICE,
                        List.of(
                                new ChoiceReqDto(1, "한식"),
                                new ChoiceReqDto(2, "양식/피자/파스타"),
                                new ChoiceReqDto(3, "치킨/패스트푸드"),
                                new ChoiceReqDto(4, "카페/디저트"),
                                new ChoiceReqDto(5, "기타")
                        )
                )
        );

        makeSurvey(questionReqDtos3, survey3, client);

        // 설문 4
        Interest interest4 = interestService.getInterest(random.nextLong(5));

        Survey survey4 = Survey.builder()
                .client(client)
                .title("2030 직장인 재테크 및 투자 성향 설문")
                .description("젊은 직장인의 재테크 방식과 투자 성향을 파악하기 위한 설문입니다.")
                .maxResponse(300)
                .reward(200L)
                .deadline(LocalDateTime.of(2026, 1, random.nextInt(1, 31), 23, 59))
                .interest(interest4)
                .state(SurveyState.IN_PROCESS)
                .responseCnt(0)
                .build();

        List<QuestionReqDto> questionReqDtos4 = List.of(
                new QuestionReqDto(
                        1,
                        "현재 주로 활용하고 있는 재테크 수단은 무엇인가요?",
                        QuestionType.SINGLE_CHOICE,
                        List.of(
                                new ChoiceReqDto(1, "예·적금"),
                                new ChoiceReqDto(2, "국내 주식"),
                                new ChoiceReqDto(3, "해외 주식/ETF"),
                                new ChoiceReqDto(4, "코인/가상자산"),
                                new ChoiceReqDto(5, "기타(부동산, 금 등)")
                        )
                ),
                new QuestionReqDto(
                        2,
                        "월 소득 대비 투자·저축 비율은 어느 정도인가요?",
                        QuestionType.SINGLE_CHOICE,
                        List.of(
                                new ChoiceReqDto(1, "10% 미만"),
                                new ChoiceReqDto(2, "10~30%"),
                                new ChoiceReqDto(3, "30~50%"),
                                new ChoiceReqDto(4, "50% 이상")
                        )
                ),
                new QuestionReqDto(
                        3,
                        "투자 의사결정을 할 때 가장 많이 참고하는 정보원은 무엇인가요?",
                        QuestionType.SINGLE_CHOICE,
                        List.of(
                                new ChoiceReqDto(1, "유튜브·SNS"),
                                new ChoiceReqDto(2, "증권사 리포트/뉴스"),
                                new ChoiceReqDto(3, "지인 추천"),
                                new ChoiceReqDto(4, "전문가 리포트/서적")
                        )
                ),
                new QuestionReqDto(
                        4,
                        "본인의 투자 성향에 가장 가까운 것은 무엇인가요?",
                        QuestionType.SINGLE_CHOICE,
                        List.of(
                                new ChoiceReqDto(1, "원금 손실은 싫고, 낮은 수익도 괜찮다"),
                                new ChoiceReqDto(2, "어느 정도 위험은 감수하지만 안정성을 중시한다"),
                                new ChoiceReqDto(3, "수익을 위해 다소 공격적인 투자를 선호한다"),
                                new ChoiceReqDto(4, "상황에 따라 단기·공격적 투자를 선호한다")
                        )
                ),
                new QuestionReqDto(
                        5,
                        "재테크를 하면서 가장 어려웠던 점이나 개선되었으면 하는 금융 서비스가 있다면 작성해 주세요.",
                        QuestionType.SUBJECTIVE,
                        List.of()
                )
        );

        makeSurvey(questionReqDtos4, survey4, client);

        // 설문 5
        Interest interest5 = interestService.getInterest(random.nextLong(5));

        Survey survey5 = Survey.builder()
                .client(client)
                .title("모바일 뱅킹 및 간편결제 서비스 이용 설문")
                .description("모바일 뱅킹과 간편결제 서비스 이용 행태를 파악하기 위한 설문입니다.")
                .maxResponse(2000)
                .reward(300L)
                .deadline(LocalDateTime.of(2025, random.nextInt(2) + 11, random.nextInt(21, 31), 23, 59))
                .interest(interest5)
                .state(SurveyState.IN_PROCESS)
                .responseCnt(0)
                .build();

        List<QuestionReqDto> questionReqDtos5 = List.of(
                new QuestionReqDto(
                        1,
                        "하루에 모바일 뱅킹 앱을 평균 몇 번 정도 이용하시나요?",
                        QuestionType.SINGLE_CHOICE,
                        List.of(
                                new ChoiceReqDto(1, "거의 이용하지 않는다"),
                                new ChoiceReqDto(2, "1~2회"),
                                new ChoiceReqDto(3, "3~5회"),
                                new ChoiceReqDto(4, "5회 이상")
                        )
                ),
                new QuestionReqDto(
                        2,
                        "가장 많이 사용하는 간편결제 서비스는 무엇인가요?",
                        QuestionType.SINGLE_CHOICE,
                        List.of(
                                new ChoiceReqDto(1, "카카오페이"),
                                new ChoiceReqDto(2, "네이버페이"),
                                new ChoiceReqDto(3, "토스"),
                                new ChoiceReqDto(4, "삼성페이/애플페이"),
                                new ChoiceReqDto(5, "기타")
                        )
                ),
                new QuestionReqDto(
                        3,
                        "모바일 금융 서비스를 사용할 때 가장 중요하게 생각하는 요소는 무엇인가요?",
                        QuestionType.SINGLE_CHOICE,
                        List.of(
                                new ChoiceReqDto(1, "보안/안전성"),
                                new ChoiceReqDto(2, "속도/편의성"),
                                new ChoiceReqDto(3, "수수료/혜택"),
                                new ChoiceReqDto(4, "UI/UX 디자인")
                        )
                ),
                new QuestionReqDto(
                        4,
                        "최근 6개월 내, 간편결제 서비스로 결제하는 비중이 어떻게 변했나요?",
                        QuestionType.SINGLE_CHOICE,
                        List.of(
                                new ChoiceReqDto(1, "이전보다 많이 늘었다"),
                                new ChoiceReqDto(2, "비슷한 수준이다"),
                                new ChoiceReqDto(3, "이전보다 줄었다"),
                                new ChoiceReqDto(4, "거의 사용하지 않는다")
                        )
                ),
                new QuestionReqDto(
                        5,
                        "모바일 뱅킹이나 간편결제 서비스에서 가장 개선되었으면 하는 점을 자유롭게 작성해 주세요.",
                        QuestionType.SUBJECTIVE,
                        List.of()
                )
        );

        makeSurvey(questionReqDtos5, survey5, client);

        // 설문 6
        Interest interest6 = interestService.getInterest(2L);

        Survey survey6 = Survey.builder()
                .client(client)
                .title("점심 메뉴 추천!")
                .description("점심 메뉴 추천을 위한 설문입니다.")
                .maxResponse(1000)
                .reward(100L)
                .deadline(LocalDateTime.of(2025, random.nextInt(2) + 11, random.nextInt(21, 31), 23, 59))
                .interest(interest6)
                .state(SurveyState.IN_PROCESS)
                .responseCnt(0)
                .build();

        List<QuestionReqDto> questionReqDtos6 = List.of(
                new QuestionReqDto(
                        1,
                        "점심 메뉴 추천해주세요!",
                        QuestionType.SUBJECTIVE,
                        List.of()
                )
        );

        makeSurvey(questionReqDtos6, survey6, client);

        return ResponseEntity.ok().build();
    }

    private void makeSurvey(List<QuestionReqDto> questionReqDtos, Survey survey, UserEntity client) {

        List<Question> questions = questionReqDtos.stream()
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
                .toList();

        survey.setQuestions(questions);
        survey.setQuestionCnt(questions.size());
        pointService.usePointsForSurvey(survey);
        surveyRepository.save(survey);
        surveyStatisticsService.createParticipantStatistics(survey);
    }

    // survey 1 1000개 더미데이터 생성
    @PostMapping("/create/dummy/1")
    public ResponseEntity<?> createDummyData1() {

        Long surveyId = 1L;
        Random random = new Random();

        for (long userId = 2L; userId <= 845L; ++userId) {
            List<AnswerReqDto> answers = new ArrayList<>();
            answers.add(new AnswerReqDto(1, QuestionType.SINGLE_CHOICE, null, List.of(random.nextInt(4) + 1)));
            answers.add(new AnswerReqDto(2, QuestionType.SINGLE_CHOICE, null, List.of(random.nextInt(4) + 1)));
            answers.add(new AnswerReqDto(3, QuestionType.SINGLE_CHOICE, null, List.of(random.nextInt(4) + 1)));
            answers.add(new AnswerReqDto(4, QuestionType.SINGLE_CHOICE, null, List.of(random.nextInt(4) + 1)));
            SurveyParticipateReqDto req = SurveyParticipateReqDto.from(answers);
            surveyService.participateSurvey(userId, surveyId, req);
        }
        return ResponseEntity.ok().build();
    }

    // survey 2
    @PostMapping("/create/dummy/2")
    public ResponseEntity<?> createDummyData2() {

        Long surveyId = 2L;
        Random random = new Random();

        for (long userId = 2L; userId <= 246L; ++userId) {
            List<AnswerReqDto> answers = new ArrayList<>();
            answers.add(new AnswerReqDto(1, QuestionType.SINGLE_CHOICE, null, List.of(random.nextInt(4) + 1)));
            answers.add(new AnswerReqDto(2, QuestionType.SINGLE_CHOICE, null, List.of(random.nextInt(4) + 1)));
            answers.add(new AnswerReqDto(3, QuestionType.SINGLE_CHOICE, null, List.of(random.nextInt(4) + 1)));
            answers.add(new AnswerReqDto(4, QuestionType.SINGLE_CHOICE, null, List.of(random.nextInt(4) + 1)));
            SurveyParticipateReqDto req = SurveyParticipateReqDto.from(answers);
            surveyService.participateSurvey(userId, surveyId, req);
        }
        return ResponseEntity.ok().build();
    }

    // survey 3
    @PostMapping("/create/dummy/3")
    public ResponseEntity<?> createDummyData3() {

        Long surveyId = 3L;
        Random random = new Random();

        for (long userId = 2L; userId <= 128L; ++userId) {
            List<AnswerReqDto> answers = new ArrayList<>();
            answers.add(new AnswerReqDto(1, QuestionType.SINGLE_CHOICE, null, List.of(random.nextInt(4) + 1)));
            answers.add(new AnswerReqDto(2, QuestionType.SINGLE_CHOICE, null, List.of(random.nextInt(4) + 1)));
            answers.add(new AnswerReqDto(3, QuestionType.SINGLE_CHOICE, null, List.of(random.nextInt(4) + 1)));
            answers.add(new AnswerReqDto(4, QuestionType.SINGLE_CHOICE, null, List.of(random.nextInt(4) + 1)));
            SurveyParticipateReqDto req = SurveyParticipateReqDto.from(answers);
            surveyService.participateSurvey(userId, surveyId, req);
        }
        return ResponseEntity.ok().build();
    }

    // survey 4
    @PostMapping("/create/dummy/4")
    public ResponseEntity<?> createDummyData4() {

        Long surveyId = 4L;
        Random random = new Random();

        for (long userId = 2L; userId <= 741; ++userId) {
            List<AnswerReqDto> answers = new ArrayList<>();
            answers.add(new AnswerReqDto(1, QuestionType.SINGLE_CHOICE, null, List.of(random.nextInt(4) + 1)));
            answers.add(new AnswerReqDto(2, QuestionType.SINGLE_CHOICE, null, List.of(random.nextInt(4) + 1)));
            answers.add(new AnswerReqDto(3, QuestionType.SINGLE_CHOICE, null, List.of(random.nextInt(4) + 1)));
            answers.add(new AnswerReqDto(4, QuestionType.SINGLE_CHOICE, null, List.of(random.nextInt(4) + 1)));
            SurveyParticipateReqDto req = SurveyParticipateReqDto.from(answers);
            surveyService.participateSurvey(userId, surveyId, req);
        }
        return ResponseEntity.ok().build();
    }

    @PostMapping("/create/dummy/menu")
    public ResponseEntity<?> createDummyMenu() {

        Long surveyId = 6L;
        Random random = new Random();
        String[] subjectivePool = {"자장면", "짬뽕", "군만두", "물", "마라탕", "백반", "초밥", "파스타", "일식", "돈까스" ,"한식"};

        for (long userId = 2L; userId <= 510L; ++userId) {
            List<AnswerReqDto> answers = new ArrayList<>();
            String subjective = subjectivePool[random.nextInt(subjectivePool.length)];
            answers.add(new AnswerReqDto(1, QuestionType.SUBJECTIVE, subjective, List.of()));
            SurveyParticipateReqDto req = SurveyParticipateReqDto.from(answers);
            surveyService.participateSurvey(userId, surveyId, req);
        }
        return ResponseEntity.ok().build();
    }
}
