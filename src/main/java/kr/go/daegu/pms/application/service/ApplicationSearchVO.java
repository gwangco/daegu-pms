package kr.go.daegu.pms.application.service;

import kr.go.daegu.pms.cmm.service.ComDefaultVO;
import lombok.Getter;
import lombok.Setter;

/**
 * 신청서 목록 검색 조건.
 * companyUserId / evaluatorUserId 는 화면에서 받지 않고(GlobalBindingAdvice 에서 바인딩 차단)
 * 서비스가 로그인 사용자의 권한에 따라 직접 채운다.
 */
@Getter
@Setter
public class ApplicationSearchVO extends ComDefaultVO {

    private static final long serialVersionUID = 1L;

    private Long announcementId;
    private String status;
    private String companyUserId;   // 기업회원: 본인 신청서만
    private String evaluatorUserId; // 평가위원: 배정받은 과제만

    /** 임시저장 제외 (담당자·평가위원). 서비스가 권한에 따라 덮어쓰므로 화면 값은 쓰이지 않는다 */
    private boolean excludeDraft;
}
