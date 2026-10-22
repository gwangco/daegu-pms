package kr.go.daegu.pms.user.service;

import kr.go.daegu.pms.config.AppProperties;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Duration;
import java.time.LocalDateTime;

/**
 * 로그인 실패 잠금 정책 (app.login.*).
 * 잠김 여부는 locked_at 과 잠금 유지 시간으로 계산한다. 시간이 지나면 별도 배치 없이 자동으로 풀린다.
 */
@Component
public class LoginLockPolicy {

    private final Clock clock;
    private final int maxFailures;
    private final Duration lockDuration;

    public LoginLockPolicy(Clock clock, AppProperties appProperties) {
        this.clock = clock;
        this.maxFailures = appProperties.login().maxFailures();
        this.lockDuration = appProperties.login().lockDuration();
    }

    public boolean isLocked(LocalDateTime lockedAt) {
        return lockedAt != null && now().isBefore(lockedAt.plus(lockDuration));
    }

    /** 잠금이 걸려 있었지만 유지 시간이 지난 경우 (다음 실패는 1회부터 다시 센다) */
    public boolean isExpired(LocalDateTime lockedAt) {
        return lockedAt != null && !isLocked(lockedAt);
    }

    /** 이 시각 이후에 잠긴 계정은 아직 잠겨 있다 (목록 조회 조건) */
    public LocalDateTime activeLockSince() {
        return now().minus(lockDuration);
    }

    public LocalDateTime unlockAt(LocalDateTime lockedAt) {
        return lockedAt == null ? null : lockedAt.plus(lockDuration);
    }

    public int getMaxFailures() {
        return maxFailures;
    }

    public long getLockMinutes() {
        return lockDuration.toMinutes();
    }

    public LocalDateTime now() {
        return LocalDateTime.now(clock);
    }
}
