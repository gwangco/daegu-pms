package kr.go.daegu.pms.cmm.mybatis;

import java.time.LocalDateTime;

/**
 * SQL 에서 #{audit.userId}, #{audit.now} 로 쓰는 감사 값.
 * AuditParameterInterceptor 가 SQL 실행 직전에 만들어 넣으므로 화면 입력으로 바꿀 수 없다.
 *
 * @param userId 로그인 사용자 ID. 배치처럼 로그인 사용자가 없으면 {@link #SYSTEM}
 * @param now    애플리케이션 Clock 기준 현재 시각 (테스트에서는 고정 시각)
 */
public record AuditInfo(String userId, LocalDateTime now) {

    public static final String SYSTEM = "SYSTEM";
}
