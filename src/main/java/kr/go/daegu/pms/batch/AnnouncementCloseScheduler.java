package kr.go.daegu.pms.batch;

import kr.go.daegu.pms.service.AnnouncementService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 공고 접수 마감 배치.
 * 매일 자정(app.batch.announcement-close-cron) 접수 마감일이 지난 OPEN 공고를 CLOSED 로 전환한다.
 *
 * 노트북처럼 자정에 서버가 꺼져 있을 수 있는 환경을 고려해 서버 기동 직후에도 1회 실행한다.
 * (실무에서 WAS 를 여러 대로 이중화하면 모든 서버가 동시에 배치를 돌리므로
 *  ShedLock 같은 분산 락이나 별도 배치 서버로 1대에서만 실행되게 해야 한다)
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AnnouncementCloseScheduler {

    private final AnnouncementService announcementService;

    @Scheduled(cron = "${app.batch.announcement-close-cron}")
    public void closeExpiredAnnouncements() {
        int closed = announcementService.closeExpiredAnnouncements();
        log.info("[배치] 접수 마감 공고 자동 전환 완료: {}건", closed);
    }

    @EventListener(ApplicationReadyEvent.class)
    public void runOnStartup() {
        closeExpiredAnnouncements();
    }
}
