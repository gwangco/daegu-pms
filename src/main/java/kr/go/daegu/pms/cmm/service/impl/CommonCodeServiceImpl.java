package kr.go.daegu.pms.cmm.service.impl;

import kr.go.daegu.pms.cmm.service.CommonCodeVO;
import kr.go.daegu.pms.cmm.service.CommonCodeService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service("commonCodeService")
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CommonCodeServiceImpl implements CommonCodeService {

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
