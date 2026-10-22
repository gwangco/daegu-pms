package kr.go.daegu.pms.config;

import kr.go.daegu.pms.announcement.batch.AnnouncementCloseScheduler;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.scheduling.annotation.Scheduled;

import java.time.ZoneId;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 업무 시각은 서버 시간대와 관계없이 한국 시간이다.
 * CI 서버(JVM 기본 시간대 UTC)에서 잠금 시각이 9시간 어긋난 일을 계기로 고정했다.
 */
class AppConfigTest {

    @Test
    @DisplayName("업무 Clock 은 서버 기본 시간대가 아니라 한국 시간(Asia/Seoul)을 쓴다")
    void businessClockUsesKoreanTime() {
        assertThat(new AppConfig().clock().getZone()).isEqualTo(ZoneId.of("Asia/Seoul"));
    }

    @Test
    @DisplayName("공고 마감 배치의 '매일 자정'도 한국 시간 기준이다")
    void closeBatchRunsAtKoreanMidnight() throws NoSuchMethodException {
        Scheduled scheduled = AnnouncementCloseScheduler.class
                .getMethod("closeExpiredAnnouncements")
                .getAnnotation(Scheduled.class);
        assertThat(scheduled.zone()).isEqualTo(AppConfig.SERVICE_ZONE_ID);
    }
}
