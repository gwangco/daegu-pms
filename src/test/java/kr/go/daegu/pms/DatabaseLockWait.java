package kr.go.daegu.pms;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

/**
 * 동시성 테스트 도우미. 두 번째 요청을 다른 스레드(다른 DB 커넥션·트랜잭션)에서 실행하고,
 * 그 요청이 첫 요청의 행 잠금을 실제로 기다리는지 MySQL performance_schema.data_lock_waits(행 잠금 대기 목록)로 확인한다.
 * 고정 sleep 으로 순서를 추측하지 않는다. (테스트 DB 계정에는 이 표의 조회 권한만 따로 준다: scripts/setup-local-mysql.ps1, CI 워크플로)
 */
final class DatabaseLockWait {

    private DatabaseLockWait() {
    }

    /** 다른 스레드에서 action 을 실행한다. user 가 null 이면 비로그인. 실패하면 그 예외를, 성공하면 null 을 돌려준다 */
    static Future<Throwable> submitAs(ExecutorService executor, String user, String role, Runnable action) {
        return executor.submit(() -> {
            if (user != null) {
                var context = SecurityContextHolder.createEmptyContext();
                context.setAuthentication(new UsernamePasswordAuthenticationToken(user, "unused",
                        List.of(new SimpleGrantedAuthority(role))));
                SecurityContextHolder.setContext(context);
            }
            try {
                action.run();
                return null;
            } catch (Throwable failure) {
                return failure;
            } finally {
                SecurityContextHolder.clearContext();
            }
        });
    }

    /** competing 이 행 잠금을 기다리기 시작하면 true, 기다리지 않고 끝나 버리거나 1초가 지나면 false */
    static boolean awaitLockWaitOrCompletion(JdbcTemplate jdbc, Future<?> competing) {
        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(1);
        while (System.nanoTime() < deadline) {
            Integer blocked = jdbc.queryForObject("SELECT COUNT(*) FROM performance_schema.data_lock_waits", Integer.class);
            if (blocked != null && blocked > 0) {
                return true;
            }
            if (competing.isDone()) {
                return false;
            }
            try {
                Thread.sleep(10);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new IllegalStateException("잠금 대기 검증 중 중단됨", e);
            }
        }
        return false;
    }
}
