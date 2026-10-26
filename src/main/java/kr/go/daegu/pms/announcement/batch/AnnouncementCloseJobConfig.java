package kr.go.daegu.pms.announcement.batch;

import kr.go.daegu.pms.announcement.service.AnnouncementVO;
import org.apache.ibatis.session.SqlSessionFactory;
import org.egovframe.rte.bat.core.item.database.EgovMyBatisBatchItemWriter;
import org.egovframe.rte.bat.core.item.database.EgovMyBatisPagingItemReader;
import org.egovframe.rte.bat.core.operation.EgovBatchSummaryListener;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;

import java.time.LocalDate;
import java.util.Map;

/**
 * 공고 접수 마감 배치 잡 (eGov 배치 = Spring Batch 5 + eGov 확장 구성요소).
 *
 *  announcementCloseJob
 *   └ announcementCloseStep (청크 100건)
 *       읽기: EgovMyBatisPagingItemReader  — 마감일(targetDate)이 지난 접수중 공고
 *       쓰기: EgovMyBatisBatchItemWriter   — 접수마감으로 변경 (JDBC 배치로 한 번에 보냄)
 *   잡 리스너: EgovBatchSummaryListener     — 끝나면 읽은·쓴 건수 요약을 로그로 남김
 *
 * 마감은 UPDATE 한 번으로도 되는 일이지만, 실행 이력(언제·어떤 날짜로·몇 건)을 남기고
 * eGov 배치의 리더·라이터 구조를 쓰려고 청크 처리로 구성했다.
 * 잡 파라미터 targetDate 가 같으면 같은 실행(JobInstance)이라 하루에 한 번만 돈다.
 */
@Configuration
public class AnnouncementCloseJobConfig {

    public static final String JOB_NAME = "announcementCloseJob";
    static final String STEP_NAME = "announcementCloseStep";
    static final int CHUNK_SIZE = 100;

    private static final String MAPPER = "kr.go.daegu.pms.announcement.service.impl.AnnouncementMapper.";

    @Bean
    public Job announcementCloseJob(JobRepository jobRepository, Step announcementCloseStep) {
        return new JobBuilder(JOB_NAME, jobRepository)
                .listener(new EgovBatchSummaryListener())
                .start(announcementCloseStep)
                .build();
    }

    @Bean
    public Step announcementCloseStep(JobRepository jobRepository,
                                      @Qualifier("txManager") PlatformTransactionManager txManager,
                                      EgovMyBatisPagingItemReader<AnnouncementVO> closeTargetReader,
                                      EgovMyBatisBatchItemWriter<AnnouncementVO> closeWriter) {
        return new StepBuilder(STEP_NAME, jobRepository)
                .<AnnouncementVO, AnnouncementVO>chunk(CHUNK_SIZE, txManager)
                .reader(closeTargetReader)
                .writer(closeWriter)
                .build();
    }

    /**
     * 마감 대상 읽기. 잡 파라미터(targetDate)를 받아야 하므로 스텝이 실행될 때 만든다(@StepScope).
     * SQL 은 OFFSET 없이 늘 첫 쪽을 읽는다: 앞 청크에서 마감한 공고는 조건(OPEN)에서 빠지므로,
     * OFFSET 으로 다음 쪽을 읽으면 아직 남은 공고를 건너뛴다 (AnnouncementCloseJobIntegrationTest 가 250건으로 확인)
     */
    @Bean
    @StepScope
    public EgovMyBatisPagingItemReader<AnnouncementVO> closeTargetReader(
            SqlSessionFactory sqlSessionFactory,
            @Value("#{jobParameters['targetDate']}") LocalDate targetDate) {
        EgovMyBatisPagingItemReader<AnnouncementVO> reader = new EgovMyBatisPagingItemReader<>();
        reader.setSqlSessionFactory(sqlSessionFactory);
        reader.setQueryId(MAPPER + "selectCloseTargetAnnouncementList");
        reader.setParameterValues(Map.of("targetDate", targetDate));
        reader.setPageSize(CHUNK_SIZE);
        return reader;
    }

    /** 마감 쓰기. 그 사이 다른 처리로 이미 마감된 공고는 바뀐 행이 0건이라도 실패로 보지 않는다 */
    @Bean
    public EgovMyBatisBatchItemWriter<AnnouncementVO> closeWriter(SqlSessionFactory sqlSessionFactory) {
        EgovMyBatisBatchItemWriter<AnnouncementVO> writer = new EgovMyBatisBatchItemWriter<>();
        writer.setSqlSessionFactory(sqlSessionFactory);
        writer.setStatementId(MAPPER + "updateAnnouncementClosedByBatch");
        writer.setAssertUpdates(false);
        return writer;
    }
}
