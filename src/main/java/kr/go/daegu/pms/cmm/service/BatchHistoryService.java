package kr.go.daegu.pms.cmm.service;

import java.util.List;

/** 배치 실행 이력 조회 (총괄관리자). 이력은 Spring Batch 가 실행할 때마다 BATCH_* 테이블에 남긴다 */
public interface BatchHistoryService {

    /** 최근 실행부터 limit 건 */
    List<BatchExecutionVO> selectBatchExecutionList(int limit);
}
