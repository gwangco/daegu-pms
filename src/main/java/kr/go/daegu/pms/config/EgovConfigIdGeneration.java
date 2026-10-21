package kr.go.daegu.pms.config;

import org.egovframe.rte.fdl.idgnr.impl.EgovTableIdGnrServiceImpl;
import org.egovframe.rte.fdl.idgnr.impl.strategy.EgovIdGnrStrategyImpl;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.sql.DataSource;

/**
 * 전자정부 ID Generation 설정 (context-idgen.xml 의 Java Config 버전).
 *
 * EgovTableIdGnrServiceImpl: IDS 테이블의 행(table_name 별 next_id)을 SELECT ... FOR UPDATE 로 잠그고 번호를 올려
 * 동시에 여러 명이 신청해도 같은 번호가 나오지 않게 채번한다.
 *
 * - 채번은 업무 트랜잭션과 별개인 새 트랜잭션(REQUIRES_NEW)에서 확정된다.
 *   → 신청 저장이 실패해 롤백되어도 번호는 소진되므로 "결번"이 생길 수 있다. (감사 시 설명 포인트)
 * - blockSize: 한 번에 DB 에서 받아 메모리에 보관할 번호 개수. 크면 DB 접근이 줄지만 서버 재시작 시 남은 번호가 버려진다.
 *   접수번호는 연속성이 중요하므로 1 로 둔다.
 */
@Configuration
public class EgovConfigIdGeneration {

    /** 신청 접수번호: DPMS-00000001 형식 */
    @Bean(destroyMethod = "destroy")
    public EgovTableIdGnrServiceImpl receiptNoIdGnrService(@Qualifier("dataSource") DataSource dataSource) {
        EgovIdGnrStrategyImpl strategy = new EgovIdGnrStrategyImpl();
        strategy.setPrefix("DPMS-");
        strategy.setCipers(8);
        strategy.setFillChar('0');

        EgovTableIdGnrServiceImpl idGnrService = new EgovTableIdGnrServiceImpl();
        idGnrService.setDataSource(dataSource);
        idGnrService.setStrategy(strategy);
        idGnrService.setBlockSize(1);
        idGnrService.setTable("ids");
        idGnrService.setTableName("APPLICATION_RECEIPT");
        return idGnrService;
    }
}
