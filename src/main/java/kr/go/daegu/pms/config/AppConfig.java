package kr.go.daegu.pms.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

import java.time.Clock;

@Configuration
@EnableScheduling
@EnableConfigurationProperties(AppProperties.class)
public class AppConfig {

    /**
     * 현재 시각을 Bean 으로 주입받기 위한 Clock.
     * 서비스에서 LocalDate.now() 를 직접 호출하면 "접수기간 마감" 같은 날짜 로직을 테스트할 수 없으므로,
     * LocalDate.now(clock) 형태로 사용하고 테스트에서는 고정된 Clock 으로 교체한다.
     */
    @Bean
    public Clock clock() {
        return Clock.systemDefaultZone();
    }
}
