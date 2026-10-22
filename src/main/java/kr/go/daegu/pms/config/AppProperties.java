package kr.go.daegu.pms.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;
import java.util.List;

/**
 * application.yml 의 app.* 설정을 타입 안전하게 바인딩하는 클래스.
 * 페이징 값은 EgovConfigProperties 가 이 값을 읽어 EgovPropertyService(pageUnit/pageSize)로 제공한다.
 */
@ConfigurationProperties(prefix = "app")
public record AppProperties(File file, Paging paging, Batch batch, Login login) {

    /** 첨부파일 저장 경로 및 허용 확장자 */
    public record File(String uploadDir, List<String> allowedExtensions) {}

    /** 목록 페이징 기본값 (pageUnit: 한 페이지 게시물 수, pageSize: 페이지 블록 크기) */
    public record Paging(int pageUnit, int pageSize) {}

    /** 배치 스케줄 */
    public record Batch(String announcementCloseCron) {}

    /** 로그인 실패 잠금 정책 (maxFailures 회 연속 실패 시 lockDuration 동안 잠금) */
    public record Login(int maxFailures, Duration lockDuration) {}
}
