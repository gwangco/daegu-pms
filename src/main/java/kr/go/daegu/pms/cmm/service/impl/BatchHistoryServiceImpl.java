package kr.go.daegu.pms.cmm.service.impl;

import kr.go.daegu.pms.cmm.message.AppMessages;
import kr.go.daegu.pms.cmm.service.BatchExecutionVO;
import kr.go.daegu.pms.cmm.service.BatchHistoryService;
import kr.go.daegu.pms.cmm.util.SecurityUtil;
import lombok.RequiredArgsConstructor;
import org.egovframe.rte.fdl.cmmn.EgovAbstractServiceImpl;
import org.springframework.stereotype.Service;

import java.util.List;

@Service("batchHistoryService")
@RequiredArgsConstructor
public class BatchHistoryServiceImpl extends EgovAbstractServiceImpl implements BatchHistoryService {

    private final BatchHistoryMapper batchHistoryMapper;
    private final AppMessages messages;

    @Override
    public List<BatchExecutionVO> selectBatchExecutionList(int limit) {
        if (!SecurityUtil.hasRole(SecurityUtil.ROLE_ADMIN)) {
            throw messages.denied("admin.denied");
        }
        return batchHistoryMapper.selectBatchExecutionList(limit);
    }
}
