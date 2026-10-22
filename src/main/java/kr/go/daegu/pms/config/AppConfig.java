package kr.go.daegu.pms.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

import java.time.Clock;
import java.time.ZoneId;

@Configuration
@EnableScheduling
@EnableConfigurationProperties(AppProperties.class)
public class AppConfig {

    /**
     * 업무 시각의 기준 시간대. 접수 마감·잠금 해제·감사 일시는 모두 한국 시간이다.
     * 서버(컨테이너) 기본 시간대는 UTC 일 수 있으므로 기본값에 맡기지 않는다. DB 접속 URL 의 connectionTimeZone 과 같다.
     */
    public static final String SERVICE_ZONE_ID = "Asia/Seoul";

    /**
     * 현재 시각을 Bean 으로 주입받기 위한 Clock.
     * 서비스에서 LocalDate.now() 를 직접 호출하면 "접수기간 마감" 같은 날짜 로직을 테스트할 수 없으므로,
     * LocalDate.now(clock) 형태로 사용하고 테스트에서는 고정된 Clock 으로 교체한다.
     */
    @Bean
    public Clock clock() {
        return Clock.system(ZoneId.of(SERVICE_ZONE_ID));
    }
}
