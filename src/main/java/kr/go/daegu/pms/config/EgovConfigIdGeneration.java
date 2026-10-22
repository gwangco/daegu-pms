package kr.go.daegu.pms.config;

import org.egovframe.rte.fdl.idgnr.impl.EgovTableIdGnrServiceImpl;
import org.egovframe.rte.fdl.idgnr.impl.strategy.EgovIdGnrStrategyImpl;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.sql.DataSource;

/**
 * 전자정부 ID Generation 설정 (공통컴포넌트 context-idgn-*.xml 의 Java Config 버전).
 *
 * EgovTableIdGnrServiceImpl: 채번 테이블(COMTECOPSEQ)의 행(TABLE_NAME 별 NEXT_ID)을 SELECT ... FOR UPDATE 로 잠그고
 * 번호를 올려, 동시에 여러 명이 신청해도 같은 번호가 나오지 않게 채번한다.
 *
 * - 공통컴포넌트가 이름으로 주입받는 Bean(egovFileIdGnrService 등)은 원본과 같은 이름·형식으로 만든다
 * - 채번은 업무 트랜잭션과 별개인 새 트랜잭션(REQUIRES_NEW)에서 확정된다.
 *   → 저장이 실패해 롤백되어도 번호는 소진되므로 "결번"이 생길 수 있다.
 * - blockSize: 한 번에 DB 에서 받아 메모리에 보관할 번호 개수. 크면 DB 접근이 줄지만 서버 재시작 시 남은 번호가 버려진다.
 */
@Configuration
public class EgovConfigIdGeneration {

    /** 공통컴포넌트 채번 테이블 */
    private static final String SEQUENCE_TABLE = "COMTECOPSEQ";

    /** 신청 접수번호: DPMS-00000001 형식. 접수번호는 연속성이 중요하므로 blockSize 1 */
    @Bean(destroyMethod = "destroy")
    public EgovTableIdGnrServiceImpl receiptNoIdGnrService(@Qualifier("dataSource") DataSource dataSource) {
        return tableIdGnrService(dataSource, "APPLICATION_RECEIPT", new EgovIdGnrStrategyImpl("DPMS-", 8, '0'), 1);
    }

    /** 첨부파일 ID: FILE_000000000000001 (공통컴포넌트 context-idgn-File.xml 과 같은 설정) */
    @Bean(destroyMethod = "destroy")
    public EgovTableIdGnrServiceImpl egovFileIdGnrService(@Qualifier("dataSource") DataSource dataSource) {
        return tableIdGnrService(dataSource, "FILE_ID", new EgovIdGnrStrategyImpl("FILE_", 15, '0'), 10);
    }

    /** 게시물 ID: 숫자 (공통컴포넌트 context-idgn-Bbs.xml 의 egovNttIdGnrService) */
    @Bean(destroyMethod = "destroy")
    public EgovTableIdGnrServiceImpl egovNttIdGnrService(@Qualifier("dataSource") DataSource dataSource) {
        return tableIdGnrService(dataSource, "NTT_ID", new EgovIdGnrStrategyImpl(), 10);
    }

    /** FAQ ID: FAQ_0000000000000001 (공통컴포넌트 context-idgn-FaqManage.xml 과 같은 설정) */
    @Bean(destroyMethod = "destroy")
    public EgovTableIdGnrServiceImpl egovFaqManageIdGnrService(@Qualifier("dataSource") DataSource dataSource) {
        return tableIdGnrService(dataSource, "FAQ_ID", new EgovIdGnrStrategyImpl("FAQ_", 16, '0'), 10);
    }

    private static EgovTableIdGnrServiceImpl tableIdGnrService(DataSource dataSource, String tableName,
                                                              EgovIdGnrStrategyImpl strategy, int blockSize) {
        EgovTableIdGnrServiceImpl idGnrService = new EgovTableIdGnrServiceImpl();
        idGnrService.setDataSource(dataSource);
        idGnrService.setStrategy(strategy);
        idGnrService.setBlockSize(blockSize);
        idGnrService.setTable(SEQUENCE_TABLE);
        idGnrService.setTableName(tableName);
        return idGnrService;
    }
}
