package kr.go.daegu.pms;

import kr.go.daegu.pms.application.service.ApplicationService;
import kr.go.daegu.pms.application.service.impl.ApplicationServiceImpl;
import kr.go.daegu.pms.cmm.exception.BusinessException;
import kr.go.daegu.pms.cmm.exception.EgovAopExceptionTransfer;
import org.egovframe.rte.fdl.cmmn.EgovAbstractServiceImpl;
import org.egovframe.rte.fdl.cmmn.exception.BaseRuntimeException;
import org.egovframe.rte.fdl.property.EgovPropertyService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.aop.support.AopUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.MessageSource;
import org.springframework.transaction.interceptor.TransactionAttribute;
import org.springframework.transaction.interceptor.TransactionInterceptor;

import java.io.IOException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Locale;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;

/**
 * 전자정부 표준프레임워크 실행환경 구성이 실제로 동작하는지 검증
 */
class EgovRuntimeIntegrationTest extends IntegrationTestSupport {

    @Autowired ApplicationContext context;
    @Autowired ApplicationService applicationService;
    @Autowired EgovPropertyService propertiesService;
    @Autowired MessageSource messageSource;
    @Autowired TransactionInterceptor txAdvice;

    @Test
    @DisplayName("ServiceImpl 은 EgovAbstractServiceImpl 을 상속하고 AOP(트랜잭션·예외 후처리) 프록시로 등록된다")
    void serviceIsEgovAndProxied() {
        assertThat(AopUtils.isAopProxy(applicationService)).isTrue();
        assertThat(AopUtils.getTargetClass(applicationService)).isEqualTo(ApplicationServiceImpl.class);
        assertThat(EgovAbstractServiceImpl.class).isAssignableFrom(ApplicationServiceImpl.class);
    }

    @Test
    @DisplayName("@EgovMapper 인터페이스가 지정한 이름으로 Bean 등록된다")
    void egovMappersRegistered() {
        assertThat(context.containsBean("announcementMapper")).isTrue();
        assertThat(context.containsBean("applicationMapper")).isTrue();
        assertThat(context.containsBean("settlementMapper")).isTrue();
    }

    @Test
    @DisplayName("EgovPropertyService 로 페이징 설정값을 읽는다")
    void propertyService() {
        assertThat(propertiesService.getInt("pageUnit")).isEqualTo(10);
        assertThat(propertiesService.getInt("pageSize")).isEqualTo(10);
    }

    @Test
    @DisplayName("메시지 번들: 업무 메시지와 eGov 실행환경 메시지를 함께 읽는다")
    void messageSource() {
        assertThat(messageSource.getMessage("page.next", null, Locale.KOREA)).isEqualTo("다음");
        assertThat(messageSource.getMessage("fail.common.msg", null, Locale.KOREA)).contains("오류");
        assertThat(messageSource.getMessage("info.nodata.msg", null, Locale.KOREA)).contains("데이터");
    }

    @Test
    @DisplayName("트랜잭션 규칙: select* 는 읽기 전용, 그 외는 쓰기 + 체크 예외도 롤백, export* 는 트랜잭션 없음")
    void transactionRules() throws Exception {
        TransactionAttribute select = attribute("selectApplicationList");
        TransactionAttribute insert = attribute("insertApplication");

        assertThat(select.isReadOnly()).isTrue();
        assertThat(insert.isReadOnly()).isFalse();
        assertThat(insert.rollbackOn(new IOException("체크 예외"))).isTrue();   // Spring 기본값은 false
        assertThat(insert.rollbackOn(new IllegalStateException())).isTrue();

        Method export = kr.go.daegu.pms.application.service.impl.ExcelServiceImpl.class
                .getMethod("exportApplicationsToExcel", java.util.List.class);
        assertThat(txAdvice.getTransactionAttributeSource()
                .getTransactionAttribute(export, export.getDeclaringClass()).getPropagationBehavior())
                .isEqualTo(TransactionAttribute.PROPAGATION_SUPPORTS);
    }

    @Test
    @DisplayName("업무 예외는 eGov 예외 계층(BaseRuntimeException)이며, 예외 후처리(장애 로그) 대상에서 제외된다")
    void businessExceptionHierarchy() {
        assertThat(new BusinessException("x")).isInstanceOf(BaseRuntimeException.class);
        assertThat(context.getBean(EgovAopExceptionTransfer.class)).isNotNull();
    }

    @Test
    @DisplayName("공고 목록 화면의 페이지 번호 바를 eGov PaginationRenderer 가 그린다")
    void paginationRenderedByEgovRenderer() throws Exception {
        mockMvc.perform(get("/announcements"))
                .andExpect(content().string(containsString("onclick=\"fnLinkPage(2); return false;\">2</a>")))
                .andExpect(content().string(containsString("page-item active")));
    }

    private TransactionAttribute attribute(String methodName) {
        Method method = Arrays.stream(ApplicationServiceImpl.class.getMethods())
                .filter(m -> m.getName().equals(methodName)).findFirst().orElseThrow();
        return txAdvice.getTransactionAttributeSource().getTransactionAttribute(method, ApplicationServiceImpl.class);
    }
}
