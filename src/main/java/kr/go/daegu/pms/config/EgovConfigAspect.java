package kr.go.daegu.pms.config;

import kr.go.daegu.pms.cmm.exception.EgovAopExceptionTransfer;
import kr.go.daegu.pms.cmm.exception.EgovServiceExceptionHandler;
import org.egovframe.rte.fdl.cmmn.aspect.ExceptionTransfer;
import org.egovframe.rte.fdl.cmmn.exception.handler.ExceptionHandler;
import org.egovframe.rte.fdl.cmmn.exception.manager.DefaultExceptionHandleManager;
import org.egovframe.rte.fdl.cmmn.exception.manager.ExceptionHandlerService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.AntPathMatcher;

/**
 * 예외 후처리 AOP 설정 (전자정부 표준 context-aspect.xml 의 Java Config 버전).
 *
 *   ServiceImpl 예외 발생
 *     → EgovAopExceptionTransfer(@AfterThrowing)
 *     → ExceptionTransfer.transfer()
 *     → DefaultExceptionHandleManager (패턴 "**service.impl.*" 에 해당하면)
 *     → EgovServiceExceptionHandler.occur()  (장애 로그/알림)
 */
@Configuration
public class EgovConfigAspect {

    @Bean
    public EgovServiceExceptionHandler egovServiceExceptionHandler() {
        return new EgovServiceExceptionHandler();
    }

    @Bean
    public DefaultExceptionHandleManager defaultExceptionHandleManager(AntPathMatcher antPathMatcher,
                                                                       EgovServiceExceptionHandler egovServiceExceptionHandler) {
        DefaultExceptionHandleManager manager = new DefaultExceptionHandleManager();
        manager.setReqExpMatcher(antPathMatcher);
        manager.setPatterns(new String[]{"**service.impl.*"});
        manager.setHandlers(new ExceptionHandler[]{egovServiceExceptionHandler});
        return manager;
    }

    @Bean
    public ExceptionTransfer exceptionTransfer(DefaultExceptionHandleManager defaultExceptionHandleManager) {
        ExceptionTransfer exceptionTransfer = new ExceptionTransfer();
        exceptionTransfer.setExceptionHandlerService(new ExceptionHandlerService[]{defaultExceptionHandleManager});
        return exceptionTransfer;
    }

    @Bean
    public EgovAopExceptionTransfer aopExceptionTransfer(ExceptionTransfer exceptionTransfer) {
        EgovAopExceptionTransfer aspect = new EgovAopExceptionTransfer();
        aspect.setExceptionTransfer(exceptionTransfer);
        return aspect;
    }
}
