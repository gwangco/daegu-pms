package kr.go.daegu.pms.cmm.service.impl;

import egovframework.com.cmm.ComDefaultCodeVO;
import egovframework.com.cmm.service.CmmnDetailCode;
import egovframework.com.cmm.service.EgovCmmUseService;
import kr.go.daegu.pms.cmm.service.CommonCodeService;
import lombok.RequiredArgsConstructor;
import org.egovframe.rte.fdl.cmmn.EgovAbstractServiceImpl;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 공통코드 조회는 공통컴포넌트 EgovCmmUseService 를 그대로 호출한다 (eGov 샘플 컨트롤러와 같은 사용법).
 */
@Service("commonCodeService")
@RequiredArgsConstructor
public class CommonCodeServiceImpl extends EgovAbstractServiceImpl implements CommonCodeService {

    private final EgovCmmUseService cmmUseService;

    @Override
    public List<CmmnDetailCode> selectCodeList(String codeId) {
        ComDefaultCodeVO codeVO = new ComDefaultCodeVO();
        codeVO.setCodeId(codeId);
        return cmmUseService.selectCmmCodeDetail(codeVO);
    }

    @Override
    public boolean isValidCode(String codeId, String code) {
        if (code == null || code.isBlank()) {
            return false;
        }
        return selectCodeList(codeId).stream().anyMatch(detail -> code.equals(detail.getCode()));
    }
}
