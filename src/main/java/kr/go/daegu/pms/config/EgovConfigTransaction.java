package kr.go.daegu.pms.config;

import org.springframework.aop.Advisor;
import org.springframework.aop.aspectj.AspectJExpressionPointcut;
import org.springframework.aop.support.DefaultPointcutAdvisor;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.interceptor.NameMatchTransactionAttributeSource;
import org.springframework.transaction.interceptor.RollbackRuleAttribute;
import org.springframework.transaction.interceptor.RuleBasedTransactionAttribute;
import org.springframework.transaction.interceptor.TransactionAttribute;
import org.springframework.transaction.interceptor.TransactionInterceptor;

import javax.sql.DataSource;
import java.util.List;
import java.util.Map;

/**
 * 선언적 트랜잭션 설정 (전자정부 표준 context-transaction.xml 의 Java Config 버전).
 *
 * 서비스 메서드마다 @Transactional 을 붙이지 않고, AOP 포인트컷으로 모든 ServiceImpl 에 일괄 적용한다.
 *   - 대상 : kr.go.daegu.pms 와 공통컴포넌트(egovframework.com) 의 *.service.impl 패키지 *Impl 클래스의 public 메서드
 *   - select* : 읽기 전용 트랜잭션
 *   - export* : 트랜잭션 없이 실행 (엑셀 생성처럼 DB 를 쓰지 않는 작업이 커넥션을 붙잡지 않도록)
 *   - 그 외   : 쓰기 트랜잭션, Exception(체크 예외 포함) 발생 시 롤백
 *
 * ※ 메서드 이름 규칙이 곧 트랜잭션 규칙이다. 데이터를 변경하는 메서드를 select 로 시작하게 지으면
 *   읽기 전용 트랜잭션에서 실행되므로 이름을 반드시 insert/update/delete 등으로 짓는다.
 * ※ 공통컴포넌트 원본 설정(context-transaction.xml)은 모든 메서드를 쓰기 트랜잭션으로 묶는다.
 *   D-PMS 규칙을 함께 적용하면서, 조회수를 같이 올리던 공통컴포넌트 메서드(selectArticleDetail 등)는
 *   조회와 조회수 증가로 나눠 호출하도록 바꿨다 (MySQL 은 읽기 전용 트랜잭션의 UPDATE 를 거부한다).
 */
@Configuration
public class EgovConfigTransaction {

    /** 서비스 구현체 전체를 가리키는 포인트컷 표현식 */
    public static final String SERVICE_IMPL_POINTCUT = "execution(* kr.go.daegu.pms..service.impl.*Impl.*(..))"
            + " || execution(* egovframework.com..service.impl.*Impl.*(..))";

    @Bean(name = "txManager")
    public DataSourceTransactionManager txManager(@Qualifier("dataSource") DataSource dataSource) {
        return new DataSourceTransactionManager(dataSource);
    }

    @Bean
    public TransactionInterceptor txAdvice(@Qualifier("txManager") DataSourceTransactionManager txManager) {
        // 기본 규칙: 트랜잭션 필수(없으면 새로 시작), 체크 예외까지 롤백 (Spring 기본값은 RuntimeException 만 롤백)
        RuleBasedTransactionAttribute writeTx = new RuleBasedTransactionAttribute();
        writeTx.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRED);
        writeTx.setRollbackRules(List.of(new RollbackRuleAttribute(Exception.class)));

        RuleBasedTransactionAttribute readOnlyTx = new RuleBasedTransactionAttribute();
        readOnlyTx.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRED);
        readOnlyTx.setReadOnly(true);
        readOnlyTx.setRollbackRules(List.of(new RollbackRuleAttribute(Exception.class)));

        RuleBasedTransactionAttribute noTx = new RuleBasedTransactionAttribute();
        noTx.setPropagationBehavior(TransactionDefinition.PROPAGATION_SUPPORTS);

        NameMatchTransactionAttributeSource source = new NameMatchTransactionAttributeSource();
        source.setNameMap(Map.<String, TransactionAttribute>of(
                "select*", readOnlyTx,
                "export*", noTx,
                "*", writeTx));

        TransactionInterceptor txAdvice = new TransactionInterceptor();
        txAdvice.setTransactionAttributeSource(source);
        txAdvice.setTransactionManager(txManager);
        return txAdvice;
    }

    @Bean
    public Advisor txAdvisor(TransactionInterceptor txAdvice) {
        AspectJExpressionPointcut pointcut = new AspectJExpressionPointcut();
        pointcut.setExpression(SERVICE_IMPL_POINTCUT);
        return new DefaultPointcutAdvisor(pointcut, txAdvice);
    }
}
