package kr.go.daegu.pms.config;

import org.egovframe.rte.fdl.property.impl.EgovPropertyServiceImpl;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.HashMap;
import java.util.Map;

/**
 * 전자정부 Property 서비스 설정 (context-properties.xml 의 Java Config 버전).
 *
 * 컨트롤러에서 propertiesService.getInt("pageUnit") 처럼 조회하는 eGov 표준 방식을 그대로 사용한다.
 * 값의 출처는 application.yml(app.paging.*)로 두어 운영 환경에서 설정 파일/환경변수로 바꿀 수 있게 했다.
 */
@Configuration
public class EgovConfigProperties {

    @Bean(destroyMethod = "destroy")
    public EgovPropertyServiceImpl propertiesService(AppProperties appProperties) {
        Map<String, String> properties = new HashMap<>();
        properties.put("pageUnit", String.valueOf(appProperties.paging().pageUnit()));   // 한 페이지 게시물 수
        properties.put("pageSize", String.valueOf(appProperties.paging().pageSize()));   // 페이지 번호 블록 크기

        EgovPropertyServiceImpl propertyService = new EgovPropertyServiceImpl();
        propertyService.setProperties(properties);
        return propertyService;
    }
}
