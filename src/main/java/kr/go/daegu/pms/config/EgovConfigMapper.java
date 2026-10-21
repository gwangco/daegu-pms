package kr.go.daegu.pms.config;

import org.apache.ibatis.session.SqlSessionFactory;
import org.mybatis.spring.SqlSessionFactoryBean;
import org.mybatis.spring.SqlSessionTemplate;
import org.egovframe.rte.psl.dataaccess.mapper.EgovMapper;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;

import javax.sql.DataSource;
import java.io.IOException;

/**
 * MyBatis 설정 (전자정부 표준프레임워크 context-mapper.xml 의 Java Config 버전).
 *
 * - sql-mapper-config.xml : MyBatis 전역 설정(언더스코어→카멜 변환, 타입 별칭 등)
 * - mappers/*_SQL.xml     : 업무별 SQL
 * - @MapperScan           : @EgovMapper 가 붙은 인터페이스를 찾아 구현체(프록시)를 Bean 으로 등록
 *
 * ※ @EgovMapper("noticeMapper") 처럼 이름을 직접 주면 Spring 6.1+ 에서 "convention-based @Component names" 폐기 경고가 난다.
 *   기본 Bean 이름이 클래스명 첫 글자 소문자(noticeMapper)라 결과가 같으므로 eGov 공식 샘플처럼 이름 없이 @EgovMapper 만 쓴다.
 */
@Configuration
@MapperScan(basePackages = "kr.go.daegu.pms", annotationClass = EgovMapper.class)
public class EgovConfigMapper {

    @Bean
    public SqlSessionFactoryBean sqlSessionFactory(@Qualifier("dataSource") DataSource dataSource) throws IOException {
        PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
        SqlSessionFactoryBean factoryBean = new SqlSessionFactoryBean();
        factoryBean.setDataSource(dataSource);
        factoryBean.setConfigLocation(resolver.getResource("classpath:/egovframework/sqlmap/sql-mapper-config.xml"));
        factoryBean.setMapperLocations(resolver.getResources("classpath:/egovframework/sqlmap/mappers/**/*_SQL.xml"));
        return factoryBean;
    }

    @Bean
    public SqlSessionTemplate sqlSessionTemplate(SqlSessionFactory sqlSessionFactory) {
        return new SqlSessionTemplate(sqlSessionFactory);
    }
}
