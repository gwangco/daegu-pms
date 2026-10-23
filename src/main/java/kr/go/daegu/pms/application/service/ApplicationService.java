package kr.go.daegu.pms.application.service;

import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Optional;

public interface ApplicationService {

    /** 로그인 사용자 권한에 맞는 신청서 목록 (기업=본인, 평가위원=배정과제, 담당자=전체) */
    List<ApplicationVO> selectApplicationList(ApplicationSearchVO searchVO);

    int selectApplicationListTotCnt(ApplicationSearchVO searchVO);

    /** 열람 권한을 검사한 뒤 신청서 상세 반환 */
    ApplicationVO selectApplication(Long applicationId);

    /** 사업계획서 제출. 생성된 신청서 ID 반환 */
    Long insertApplication(ApplicationVO application, MultipartFile planFile);

    /** 임시저장 (처음 쓰는 신청서). 과제명만 있으면 저장되고, 사업계획서는 고른 경우에만 함께 저장한다 */
    Long insertApplicationDraft(ApplicationVO application, MultipartFile planFile);

    /** 이어서 작성할 내 임시저장 신청서 (본인 것, 임시저장 상태만) */
    ApplicationVO selectApplicationDraft(Long applicationId);

    /**
     * 임시저장 신청서 고치기. submit 이 true 면 필수 항목·금액을 모두 확인한 뒤 제출한다
     * (처음 제출이면 접수번호 발급, 제출 취소 후 다시 내면 같은 접수번호)
     */
    void updateApplicationDraft(Long applicationId, ApplicationVO application, MultipartFile planFile, boolean submit);

    /** 제출 취소: 서류 검토 전(접수완료)이고 접수 기간 안이면 임시저장으로 되돌린다 */
    void updateApplicationWithdraw(Long applicationId);

    /** 임시저장 신청서 삭제 (본인 것, 임시저장 상태만) */
    void deleteApplicationDraft(Long applicationId);

    /** 이 공고에 낸 내 신청서 (없으면 빈 값). 신청 화면에서 이미 쓴 신청서로 보내는 데 쓴다 */
    Optional<ApplicationVO> selectMyApplicationByAnnouncement(Long announcementId);

    /** 담당자의 심사 상태 변경 (상태머신 규칙 검증) */
    void updateApplicationStatus(Long applicationId, String statusCode);

    int selectTotalApplicationCount();

    int selectSelectedApplicationCount();
}
