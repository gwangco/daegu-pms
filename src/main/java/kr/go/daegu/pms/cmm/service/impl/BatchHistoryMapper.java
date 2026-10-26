package kr.go.daegu.pms.cmm.service.impl;

import kr.go.daegu.pms.cmm.service.BatchExecutionVO;
import org.apache.ibatis.annotations.Param;
import org.egovframe.rte.psl.dataaccess.mapper.EgovMapper;

import java.util.List;

@EgovMapper
public interface BatchHistoryMapper {

    List<BatchExecutionVO> selectBatchExecutionList(@Param("limit") int limit);
}
