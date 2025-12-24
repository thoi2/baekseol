package com.example.backend.survey.config;

import com.example.backend.point.service.PointService;
import com.example.backend.survey.entity.Survey;
import com.example.backend.survey.enumType.SurveyState;
import com.example.backend.survey.repository.SurveyRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.JobScope;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.launch.support.RunIdIncrementer;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.batch.item.data.RepositoryItemReader;
import org.springframework.batch.item.data.RepositoryItemWriter;
import org.springframework.batch.item.data.builder.RepositoryItemReaderBuilder;
import org.springframework.batch.item.data.builder.RepositoryItemWriterBuilder;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.Sort;
import org.springframework.transaction.PlatformTransactionManager;

import java.time.LocalDateTime;
import java.util.Map;

@Slf4j
@Configuration
@RequiredArgsConstructor
public class SurveyDeadlineBatchConfig {

    private static final int CHUNK_SIZE = 100;
    private final JobRepository jobRepository;
    private final PlatformTransactionManager platformTransactionManager;
    private final SurveyRepository surveyRepository;
    private final PointService pointService;

    // 설문 마감 처리 작업
    @Bean
    public Job surveyDeadlineJob() {
        return new JobBuilder("surveyDeadlineJob", jobRepository)
                .incrementer(new RunIdIncrementer())
                .start(surveyDeadlineStep())
                .build();
    }

    @Bean
    @JobScope
    public Step surveyDeadlineStep() {
        return new StepBuilder("surveyDeadlineStep", jobRepository)
                .tasklet((contribution, chunkContext) -> {
                    surveyRepository.bulkUpdateExpiredSurveys(
                            SurveyState.DONE,
                            SurveyState.IN_PROCESS,
                            LocalDateTime.now()
                    );
                    return RepeatStatus.FINISHED;
                }, platformTransactionManager)
                .build();
    }

    // 마감 시간이 지난 IN_PROCESS 설문 읽기
    @Bean
    @StepScope
    public RepositoryItemReader<Survey> expiredSurveyReader() {
        return new RepositoryItemReaderBuilder<Survey>()
                .name("expiredSurveyReader")
                .repository(surveyRepository)
                .methodName("findByStateAndDeadlineBefore")
                .arguments(SurveyState.IN_PROCESS, LocalDateTime.now())
                .pageSize(CHUNK_SIZE)
                .sorts(Map.of("surveyId", Sort.Direction.ASC))
                .build();
    }

    // 설문의 상태를 DONE으로 변경
    @Bean
    @StepScope
    public ItemProcessor<Survey, Survey> surveyDeadlineProcessor() {
        return survey -> {
            survey.setState(SurveyState.DONE);
            pointService.refundSPointsForSurvey(survey);
            return survey;
        };
    }

    // 변경된 설문 저장
    @Bean
    @StepScope
    public RepositoryItemWriter<Survey> surveyDeadlineWriter() {
        return new RepositoryItemWriterBuilder<Survey>()
                .repository(surveyRepository)
                .methodName("save")
                .build();
    }

    // TODO: 이메일 발송 STEP 추가
}
