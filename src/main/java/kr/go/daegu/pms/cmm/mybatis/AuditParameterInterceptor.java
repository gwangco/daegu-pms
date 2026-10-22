package kr.go.daegu.pms.cmm.mybatis;

import kr.go.daegu.pms.cmm.util.SecurityUtil;
import org.apache.ibatis.executor.statement.StatementHandler;
import org.apache.ibatis.plugin.Interceptor;
import org.apache.ibatis.plugin.Intercepts;
import org.apache.ibatis.plugin.Invocation;
import org.apache.ibatis.plugin.Signature;

import java.sql.Statement;
import java.time.Clock;
import java.time.LocalDateTime;

/**
 * 수정자·수정일시 같은 감사 컬럼 값을 SQL 마다 자동으로 넣어 주는 MyBatis 플러그인.
 *
 * PreparedStatement 에 값을 바인딩하기 직전(StatementHandler.parameterize)에
 * BoundSql 의 추가 파라미터로 "audit" 를 넣는다. 그래서 Mapper XML 은 파라미터 타입(VO, @Param Map)과 관계없이
 *   UPDATE ... SET updated_by = #{audit.userId}, updated_at = #{audit.now}
 * 처럼 같은 방법으로 쓸 수 있고, 서비스 코드는 수정자를 직접 채우지 않아도 된다.
 *
 * 값은 SecurityContext 와 Clock 에서만 가져오므로 요청 파라미터로 위조할 수 없다.
 */
@Intercepts(@Signature(type = StatementHandler.class, method = "parameterize", args = Statement.class))
public class AuditParameterInterceptor implements Interceptor {

    public static final String PARAMETER_NAME = "audit";

    private final Clock clock;

    public AuditParameterInterceptor(Clock clock) {
        this.clock = clock;
    }

    @Override
    public Object intercept(Invocation invocation) throws Throwable {
        StatementHandler handler = (StatementHandler) invocation.getTarget();
        handler.getBoundSql().setAdditionalParameter(PARAMETER_NAME, current());
        return invocation.proceed();
    }

    AuditInfo current() {
        String userId = SecurityUtil.getCurrentUserId();
        return new AuditInfo(userId == null ? AuditInfo.SYSTEM : userId, LocalDateTime.now(clock));
    }
}
