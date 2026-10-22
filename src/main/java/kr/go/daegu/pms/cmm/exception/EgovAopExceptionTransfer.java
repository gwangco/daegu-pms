package kr.go.daegu.pms.cmm.exception;

import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.AfterThrowing;
import org.aspectj.lang.annotation.Aspect;
import org.egovframe.rte.fdl.cmmn.aspect.ExceptionTransfer;
import org.springframework.security.access.AccessDeniedException;

/**
 * 서비스 예외 후처리 Aspect (전자정부 표준 context-aspect.xml 의 aop:after-throwing 설정).
 *
 * ServiceImpl 에서 예외가 발생하면 eGov ExceptionTransfer 로 넘겨 로그 기록·후처리기(EgovServiceExceptionHandler)를 실행한다.
 * ExceptionTransfer 는 DB 예외(DataAccessException)를 SQL 정보가 없는 일반 메시지 예외로 바꿔 던져 내부 정보 노출도 막아 준다.
 *
 * [커스터마이징] 업무 규칙 위반(BusinessException), 데이터 없음(NotFoundException), 권한 없음(AccessDeniedException)은
 * 정상적인 업무 흐름이므로 후처리 대상에서 제외한다.
 * eGov ExceptionTransfer 는 모든 RuntimeException 을 ERROR 로그로 남기기 때문에, 제외하지 않으면
 * "잔액 초과" 같은 안내까지 장애 로그로 쌓여 진짜 장애를 찾기 어려워진다.
 * (@AfterThrowing 은 예외를 삼키지 못한다. 여기서 그냥 return 해도 원래 예외는 그대로 호출자에게 전달된다)
 */
@Aspect
public class EgovAopExceptionTransfer {

    private ExceptionTransfer exceptionTransfer;

    public void setExceptionTransfer(ExceptionTransfer exceptionTransfer) {
        this.exceptionTransfer = exceptionTransfer;
    }

    /** 업무 서비스와 공통컴포넌트 서비스 모두 (트랜잭션 포인트컷과 같은 범위) */
    @AfterThrowing(pointcut = "execution(* kr.go.daegu.pms..service.impl.*Impl.*(..))"
            + " || execution(* egovframework.com..service.impl.*Impl.*(..))", throwing = "ex")
    public void doAfterThrowingExceptionTransferService(JoinPoint joinPoint, Exception ex) throws Exception {
        if (isExpectedBusinessFlow(ex)) {
            return;
        }
        exceptionTransfer.transfer(joinPoint, ex);
    }

    static boolean isExpectedBusinessFlow(Exception ex) {
        return ex instanceof BusinessException
                || ex instanceof NotFoundException
                || ex instanceof AccessDeniedException;
    }
}
