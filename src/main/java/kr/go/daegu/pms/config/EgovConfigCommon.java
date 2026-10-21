package kr.go.daegu.pms.config;

import org.egovframe.rte.fdl.cmmn.trace.LeaveaTrace;
import org.egovframe.rte.fdl.cmmn.trace.handler.DefaultTraceHandler;
import org.egovframe.rte.fdl.cmmn.trace.handler.TraceHandler;
import org.egovframe.rte.fdl.cmmn.trace.manager.DefaultTraceHandleManager;
import org.egovframe.rte.fdl.cmmn.trace.manager.TraceHandlerService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.support.MessageSourceAccessor;
import org.springframework.context.support.ReloadableResourceBundleMessageSource;
import org.springframework.util.AntPathMatcher;

/**
 * 전자정부 표준프레임워크 공통 설정 (context-common.xml 의 Java Config 버전).
 *
 * - messageSource : 메시지 번들. EgovAbstractServiceImpl, ExceptionTransfer 가 이름("messageSource")으로 주입받는다
 * - leaveaTrace   : EgovAbstractServiceImpl 이 이름("leaveaTrace")으로 주입받는 Trace(추적 로그) 처리기
 *
 * ※ 위 두 Bean 이 없으면 EgovAbstractServiceImpl 을 상속한 모든 서비스가 기동 시 주입 오류로 실패한다.
 */
@Configuration
public class EgovConfigCommon {

    @Bean
    public AntPathMatcher antPathMatcher() {
        return new AntPathMatcher();
    }

    @Bean
    public ReloadableResourceBundleMessageSource messageSource() {
        ReloadableResourceBundleMessageSource messageSource = new ReloadableResourceBundleMessageSource();
        messageSource.setBasenames(
                // 우리 업무 메시지 (먼저 선언한 번들이 우선)
                "classpath:/egovframework/message/message-common",
                // eGovFrame 실행환경 기본 메시지 (fail.common.msg 등)
                "classpath:/messages/message-common",
                "classpath:/org/egovframe/rte/fdl/idgnr/messages/idgnr",
                "classpath:/org/egovframe/rte/fdl/property/messages/properties");
        messageSource.setDefaultEncoding("UTF-8");
        messageSource.setCacheSeconds(60);
        // useCodeAsDefaultMessage 는 기본값(false) 유지: 없는 메시지 키는 예외가 발생하므로 키 누락을 테스트에서 바로 발견할 수 있다
        return messageSource;
    }

    @Bean
    public MessageSourceAccessor messageSourceAccessor() {
        return new MessageSourceAccessor(messageSource());
    }

    @Bean
    public DefaultTraceHandler defaultTraceHandler() {
        return new DefaultTraceHandler();
    }

    @Bean
    public DefaultTraceHandleManager traceHandlerService() {
        DefaultTraceHandleManager manager = new DefaultTraceHandleManager();
        manager.setReqExpMatcher(antPathMatcher());
        manager.setPatterns(new String[]{"*"});
        manager.setHandlers(new TraceHandler[]{defaultTraceHandler()});
        return manager;
    }

    @Bean
    public LeaveaTrace leaveaTrace() {
        LeaveaTrace leaveaTrace = new LeaveaTrace();
        leaveaTrace.setTraceHandlerServices(new TraceHandlerService[]{traceHandlerService()});
        return leaveaTrace;
    }
}
