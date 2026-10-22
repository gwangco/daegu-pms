package kr.go.daegu.pms;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;

/**
 * 통합 테스트 공통 설정.
 * - local,test 프로필: MySQL 테스트 전용 DB(dpms_test)를 쓰고, 컨텍스트가 뜰 때 비우고 다시 만든다 (TestDatabaseConfig)
 * - 각 테스트는 트랜잭션 안에서 실행되고 끝나면 롤백 → seed 데이터가 항상 같은 상태로 유지
 * - 현재 날짜를 2026-10-21 로 고정 → 언제 실행해도 "접수기간" 관련 결과가 같음
 * - 업로드 파일은 build/test-uploads 에 저장 (롤백 시 자동 삭제)
 */
@SpringBootTest(properties = "app.file.upload-dir=build/test-uploads")
@ActiveProfiles({"local", "test"})
@AutoConfigureMockMvc
@Transactional
@Import({IntegrationTestSupport.FixedClockConfig.class, TestDatabaseConfig.class})
public abstract class IntegrationTestSupport {

    protected static final byte[] PDF_BYTES = "%PDF-1.4\n% test document\n".getBytes(StandardCharsets.US_ASCII);

    @Autowired
    protected MockMvc mockMvc;

    protected static MockMultipartFile pdf(String paramName, String fileName) {
        return new MockMultipartFile(paramName, fileName, "application/pdf", PDF_BYTES);
    }

    @TestConfiguration
    static class FixedClockConfig {
        @Bean
        @Primary
        Clock fixedClock() {
            return Clock.fixed(Instant.parse("2026-10-21T01:00:00Z"), ZoneId.of("Asia/Seoul"));
        }
    }
}
