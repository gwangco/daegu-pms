package kr.go.daegu.pms.config;

import kr.go.daegu.pms.cmm.pagination.EgovBootstrapPaginationRenderer;
import kr.go.daegu.pms.cmm.pagination.EgovPaginationDialect;
import org.springframework.context.MessageSource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 화면(Thymeleaf) 관련 전자정부 설정.
 * - 페이지 번호 바 렌더러(PaginationRenderer)와 이를 템플릿에서 쓰기 위한 Dialect 를 등록한다.
 */
@Configuration
public class EgovConfigWeb {

    @Bean
    public EgovBootstrapPaginationRenderer egovPaginationRenderer(MessageSource messageSource) {
        return new EgovBootstrapPaginationRenderer(messageSource);
    }

    @Bean
    public EgovPaginationDialect egovPaginationDialect(EgovBootstrapPaginationRenderer egovPaginationRenderer) {
        return new EgovPaginationDialect(egovPaginationRenderer);
    }
}
