package kr.go.daegu.pms.domain;

import kr.go.daegu.pms.common.vo.SearchVO;
import lombok.Getter;
import lombok.Setter;

/**
 * 신청서 목록 검색 조건.
 * companyUserId / evaluatorUserId 는 화면에서 받지 않고(GlobalBindingAdvice 에서 바인딩 차단)
 * 서비스가 로그인 사용자의 권한에 따라 직접 채운다.
 */
@Getter
@Setter
public class ApplicationSearchVO extends SearchVO {
    private Long announcementId;
    private String status;
    private String companyUserId;   // 기업회원: 본인 신청서만
    private String evaluatorUserId; // 평가위원: 배정받은 과제만
}
