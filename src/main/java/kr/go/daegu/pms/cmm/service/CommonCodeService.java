package kr.go.daegu.pms.cmm.service;


import java.util.List;

/**
 * 공통코드 조회 서비스 (전자정부 공통컴포넌트 EgovCmmUseService 의 역할)
 */
public interface CommonCodeService {

    String GROUP_FIELD = "FIELD";
    String GROUP_EXP_ITEM = "EXP_ITEM";
    String GROUP_FAQ_CATEGORY = "FAQ_CATEGORY";

    List<CommonCodeVO> selectCodeList(String groupId);

    /** 화면에서 넘어온 코드값이 실제 사용중인 공통코드인지 검증 */
    boolean isValidCode(String groupId, String codeId);
}
