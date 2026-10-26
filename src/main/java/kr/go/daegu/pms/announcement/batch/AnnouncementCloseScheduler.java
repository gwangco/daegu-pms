package kr.go.daegu.pms.announcement.batch;

import kr.go.daegu.pms.config.AppConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.JobParametersInvalidException;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.batch.core.repository.JobExecutionAlreadyRunningException;
import org.springframework.batch.core.repository.JobInstanceAlreadyCompleteException;
import org.springframework.batch.core.repository.JobRestartException;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;

/**
 * 공고 접수 마감 배치 실행.
 * 매일 자정(app.batch.announcement-close-cron, 한국 시간) eGov 배치 잡 announcementCloseJob 을 오늘 날짜로 실행한다.
 * 노트북처럼 자정에 서버가 꺼져 있을 수 있는 환경을 고려해 서버 기동 직후에도 1회 실행한다.
 *
 * 잡 파라미터 targetDate(오늘)가 실행을 구분한다. 같은 날 두 번째 실행은 Spring Batch 가 "이미 끝난 실행"으로 거부하므로
 * 기동 직후 실행과 자정 실행이 겹쳐도 한 번만 처리된다.
 * 총괄관리자의 "지금 실행"은 요청 시각(requestedAt)을 함께 넘겨 같은 날에도 새 실행으로 돌린다.
 * (실무에서 WAS 를 여러 대로 이중화하면 모든 서버가 동시에 배치를 돌리므로 별도 배치 서버나 분산 락으로 1대에서만 실행되게 한다)
 *
 * 잡은 스케줄러·컨트롤러에서 직접 실행한다. 서비스(트랜잭션 AOP) 안에서 실행하면 Spring Batch 가
 * "이미 트랜잭션이 있다"며 거부한다(JobRepository 는 자기 트랜잭션으로 실행 이력을 남긴다).
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AnnouncementCloseScheduler {

    private final JobLauncher jobLauncher;
    private final Job announcementCloseJob;
    private final Clock clock;

    @Scheduled(cron = "${app.batch.announcement-close-cron}", zone = AppConfig.SERVICE_ZONE_ID)
    public void closeExpiredAnnouncements() {
        runDaily(LocalDate.now(clock));
    }

    @EventListener(ApplicationReadyEvent.class)
    public void runOnStartup() {
        closeExpiredAnnouncements();
    }

    /** 날짜별 정기 실행. 그 날짜로 이미 끝난 실행이 있으면 건너뛰고 빈 값을 돌려준다 */
    public Optional<JobExecution> runDaily(LocalDate targetDate) {
        JobParameters parameters = new JobParametersBuilder()
                .addLocalDate("targetDate", targetDate)
                .toJobParameters();
        try {
            return Optional.of(launch(parameters));
        } catch (JobInstanceAlreadyCompleteException e) {
            log.info("[배치] 공고 마감: {} 실행은 이미 끝나 건너뜀", targetDate);
            return Optional.empty();
        }
    }

    /** 총괄관리자의 수동 실행. 요청 시각을 식별 파라미터로 넣어 같은 날에도 새로 실행한다 */
    public JobExecution runNow(String requestedBy) throws JobInstanceAlreadyCompleteException {
        JobParameters parameters = new JobParametersBuilder()
                .addLocalDate("targetDate", LocalDate.now(clock))
                .addLocalDateTime("requestedAt", LocalDateTime.now(clock))
                .addString("requestedBy", requestedBy, false)
                .toJobParameters();
        return launch(parameters);
    }

    private JobExecution launch(JobParameters parameters) throws JobInstanceAlreadyCompleteException {
        try {
            JobExecution execution = jobLauncher.run(announcementCloseJob, parameters);
            log.info("[배치] 공고 마감 실행 #{}: {} (파라미터 {})", execution.getId(), execution.getStatus(), parameters);
            return execution;
        } catch (JobExecutionAlreadyRunningException | JobRestartException | JobParametersInvalidException e) {
            throw new IllegalStateException("공고 마감 배치를 실행하지 못했습니다.", e);
        }
    }
}
