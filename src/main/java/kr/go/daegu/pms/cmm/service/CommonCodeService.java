package kr.go.daegu.pms.cmm.service;

import egovframework.com.cmm.service.CmmnDetailCode;

import java.util.List;

/**
 * 업무 공통코드 조회·검증.
 *
 * 코드 조회는 전자정부 공통컴포넌트 EgovCmmUseService(COMTCCMMNDETAILCODE)에 맡기고,
 * 여기서는 D-PMS 코드 ID 상수와 "화면에서 넘어온 값이 사용 중인 코드인지" 서버 검증만 담당한다.
 * 코드 ID 는 공통컴포넌트 규칙(분류코드 3자리 + 일련번호 3자리)을 따른다.
 */
public interface CommonCodeService {

    /** 지원 분야 (MOBILITY, ROBOT, AI_SW, HEALTH, SMART_FAB) */
    String FIELD = "DPM001";
    /** 신청서 진행상태 */
    String APP_STATUS = "DPM002";
    /** 사업비 비목 (EXP01 ~ EXP05) */
    String EXP_ITEM = "DPM003";
    /** 정산 검수상태 */
    String SETTLE_STATUS = "DPM004";
    /** FAQ 분류 (APPLY, REVIEW, SETTLE, ACCOUNT) */
    String FAQ_CATEGORY = "DPM005";

    /** 사용 중인 상세코드 목록 (표시 순서대로) → 화면 선택 상자 */
    List<CmmnDetailCode> selectCodeList(String codeId);

    /** 화면에서 넘어온 코드값이 사용 중인 공통코드인지 검증 */
    boolean isValidCode(String codeId, String code);
}
