package kr.go.daegu.pms;

import egovframework.com.cmm.service.CmmnDetailCode;
import egovframework.com.cmm.service.EgovCmmUseService;
import egovframework.com.cmm.service.EgovFileMngService;
import egovframework.com.cop.bbs.service.BoardVO;
import egovframework.com.cop.bbs.service.EgovArticleService;
import egovframework.com.cop.bbs.service.impl.EgovArticleServiceImpl;
import egovframework.com.uss.olh.faq.service.EgovFaqService;
import kr.go.daegu.pms.cmm.service.CommonCodeService;
import kr.go.daegu.pms.notice.service.NoticeService;
import org.egovframe.rte.fdl.cmmn.EgovAbstractServiceImpl;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.aop.support.AopUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.interceptor.TransactionAttribute;
import org.springframework.transaction.interceptor.TransactionInterceptor;
import org.springframework.transaction.support.TransactionTemplate;

import java.sql.SQLException;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * 전자정부 공통컴포넌트 도입(6차 스프린트) 검증.
 * - 공통컴포넌트 서비스가 Bean 으로 등록되고 D-PMS 트랜잭션 규칙(select* 읽기 전용)이 그대로 적용되는지
 * - V2 마이그레이션이 옛 테이블의 데이터를 공통컴포넌트 테이블로 빠짐없이 옮겼는지
 */
class CommonComponentIntegrationTest extends IntegrationTestSupport {

    @Autowired EgovArticleService articleService;
    @Autowired EgovFaqService faqService;
    @Autowired EgovFileMngService fileMngService;
    @Autowired EgovCmmUseService cmmUseService;
    @Autowired CommonCodeService commonCodeService;
    @Autowired TransactionInterceptor txAdvice;
    @Autowired PlatformTransactionManager txManager;
    @Autowired JdbcTemplate jdbc;

    @Test
    @DisplayName("공통컴포넌트 서비스도 EgovAbstractServiceImpl 을 상속하고 트랜잭션·예외 후처리 AOP 프록시로 등록된다")
    void commonComponentServicesAreProxied() {
        for (Object service : List.of(articleService, faqService, fileMngService, cmmUseService)) {
            assertThat(AopUtils.isAopProxy(service)).as(service.getClass().getName()).isTrue();
            assertThat(AopUtils.getTargetClass(service)).isAssignableTo(EgovAbstractServiceImpl.class);
        }
    }

    @Test
    @DisplayName("공통컴포넌트 메서드에도 D-PMS 규칙 적용: selectArticleList 는 읽기 전용, increaseInqireCo 는 쓰기")
    void transactionRuleAppliesToCommonComponents() throws Exception {
        TransactionAttribute select = txAdvice.getTransactionAttributeSource().getTransactionAttribute(
                EgovArticleServiceImpl.class.getMethod("selectArticleList", BoardVO.class), EgovArticleServiceImpl.class);
        TransactionAttribute increase = txAdvice.getTransactionAttributeSource().getTransactionAttribute(
                EgovArticleServiceImpl.class.getMethod("increaseInqireCo", BoardVO.class), EgovArticleServiceImpl.class);
        assertThat(select.isReadOnly()).isTrue();
        assertThat(increase.isReadOnly()).isFalse();
    }

    @Test
    @DisplayName("MySQL 드라이버는 읽기 전용 트랜잭션 안의 UPDATE 를 거부한다 (H2 는 허용했다) → 조회수 증가는 select* 와 나눠 호출")
    void mysqlRejectsUpdateInReadOnlyTransaction() {
        TransactionTemplate readOnly = new TransactionTemplate(txManager);
        readOnly.setReadOnly(true);
        readOnly.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);

        BoardVO notice = new BoardVO();
        notice.setBbsId(NoticeService.BBS_ID);
        notice.setNttId(3L);

        // 공통컴포넌트 원본 selectArticleDetail 처럼 "읽기 전용 트랜잭션 안에서 조회수 UPDATE" 를 재현한다.
        // Connector/J 는 읽기 전용 연결에서 데이터를 바꾸는 SQL 을 서버에 보내기 전에 막는다
        assertThatThrownBy(() -> readOnly.executeWithoutResult(status -> articleService.increaseInqireCo(notice)))
                .rootCause()
                .isInstanceOf(SQLException.class)
                .hasMessageContaining("read-only");
    }

    @Test
    @DisplayName("공통코드: 공통컴포넌트 EgovCmmUseService 로 조회하고 SORT_ORDR(D-PMS 추가 컬럼) 순서로 나온다")
    void commonCodesFromCommonComponent() {
        List<String> fields = commonCodeService.selectCodeList(CommonCodeService.FIELD).stream()
                .map(CmmnDetailCode::getCode)
                .toList();
        assertThat(fields).containsExactly("MOBILITY", "ROBOT", "AI_SW", "HEALTH", "SMART_FAB");
        assertThat(commonCodeService.isValidCode(CommonCodeService.EXP_ITEM, "EXP03")).isTrue();
        assertThat(commonCodeService.isValidCode(CommonCodeService.EXP_ITEM, "exp03")).isFalse();
    }

    @Test
    @DisplayName("V2 마이그레이션: 옛 자체 테이블은 없어지고 데이터는 공통컴포넌트 테이블로 옮겨졌다")
    void v2MigrationMovedEverything() {
        Integer oldTables = jdbc.queryForObject("""
                SELECT COUNT(*) FROM information_schema.tables
                WHERE table_schema = DATABASE()
                  AND LOWER(table_name) IN ('notice', 'faq', 'com_file', 'ids', 'com_code_group', 'com_code_detail')
                """, Integer.class);
        assertThat(oldTables).isZero();

        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM COMTCCMMNCODE WHERE CL_CODE = 'DPM'", Integer.class)).isEqualTo(5);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM COMTCCMMNDETAILCODE WHERE CODE_ID LIKE 'DPM%'", Integer.class)).isEqualTo(23);

        // 공지 3건 → 공지 게시판 게시물 1~3 (중요 공지 여부 유지)
        assertThat(jdbc.queryForList("SELECT NTT_ID FROM COMTNBBS WHERE BBS_ID = ? AND NOTICE_AT = 'Y' ORDER BY NTT_ID",
                Long.class, NoticeService.BBS_ID)).containsExactly(1L, 2L);
        // FAQ 8건 → FAQ_ + 16자리 ID
        assertThat(jdbc.queryForList("SELECT FAQ_ID FROM COMTNFAQINFO ORDER BY FAQ_ID", String.class))
                .hasSizeGreaterThanOrEqualTo(8)
                .allMatch(id -> id.matches("FAQ_\\d{16}"));

        // 채번: 옮긴 데이터 다음 번호부터 발급 (테스트 중 채번은 롤백돼도 소진되므로 "이상"으로 확인)
        assertThat(nextId("APPLICATION_RECEIPT")).isGreaterThanOrEqualTo(4);
        assertThat(nextId("NTT_ID")).isGreaterThanOrEqualTo(4);
        assertThat(nextId("FAQ_ID")).isGreaterThanOrEqualTo(9);
        assertThat(nextId("FILE_ID")).isGreaterThanOrEqualTo(1);

        assertThat(jdbc.queryForList("SELECT version FROM flyway_schema_history WHERE success = 1 ORDER BY installed_rank", String.class))
                .containsExactly("1", "1.1", "2", "3", "4", "4.1", "5", "6", "7");
    }

    private long nextId(String tableName) {
        Long next = jdbc.queryForObject("SELECT NEXT_ID FROM COMTECOPSEQ WHERE TABLE_NAME = ?", Long.class, tableName);
        return next == null ? 0 : next;
    }
}
