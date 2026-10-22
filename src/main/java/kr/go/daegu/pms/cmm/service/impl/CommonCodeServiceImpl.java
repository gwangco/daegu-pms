package kr.go.daegu.pms.cmm.service.impl;

import kr.go.daegu.pms.cmm.service.CommonCodeService;
import kr.go.daegu.pms.cmm.service.CommonCodeVO;
import lombok.RequiredArgsConstructor;
import org.egovframe.rte.fdl.cmmn.EgovAbstractServiceImpl;
import org.springframework.stereotype.Service;

import java.util.List;

@Service("commonCodeService")
@RequiredArgsConstructor
public class CommonCodeServiceImpl extends EgovAbstractServiceImpl implements CommonCodeService {

    private final CommonCodeMapper commonCodeMapper;

    @Override
    public List<CommonCodeVO> selectCodeList(String groupId) {
        return commonCodeMapper.selectCodeList(groupId);
    }

    @Override
    public boolean isValidCode(String groupId, String codeId) {
        if (codeId == null || codeId.isBlank()) {
            return false;
        }
        return commonCodeMapper.selectCodeCount(groupId, codeId) > 0;
    }
}
