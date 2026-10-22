package kr.go.daegu.pms.settlement.service;

import kr.go.daegu.pms.cmm.service.ComDefaultVO;
import lombok.Getter;
import lombok.Setter;

/**
 * 정산 목록 검색 조건.
 * 과제별 정산 화면은 applicationId 를 URL 경로로만 정하고(화면 값 무시), 담당자 검수함은 applicationId 없이 전체를 본다.
 */
@Getter
@Setter
public class SettlementSearchVO extends ComDefaultVO {

    private static final long serialVersionUID = 1L;

    private Long applicationId;
    private String status;
}
