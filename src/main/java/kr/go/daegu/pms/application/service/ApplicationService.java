package kr.go.daegu.pms.application.service;

import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface ApplicationService {

    /** 로그인 사용자 권한에 맞는 신청서 목록 (기업=본인, 평가위원=배정과제, 담당자=전체) */
    List<ApplicationVO> selectApplicationList(ApplicationSearchVO searchVO);

    int selectApplicationListTotCnt(ApplicationSearchVO searchVO);

    /** 열람 권한을 검사한 뒤 신청서 상세 반환 */
    ApplicationVO selectApplication(Long applicationId);

    /** 사업계획서 제출. 생성된 신청서 ID 반환 */
    Long insertApplication(ApplicationVO application, MultipartFile planFile);

    /** 담당자의 심사 상태 변경 (상태머신 규칙 검증) */
    void updateApplicationStatus(Long applicationId, String statusCode);

    int selectTotalApplicationCount();

    int selectSelectedApplicationCount();
}
