package kr.go.daegu.pms.user.service;

/**
 * 기업회원 가입 (비로그인 사용자).
 * 공통컴포넌트의 기업회원 관리(uss.umt EgovEntrprsManageService)에 해당하는 기능을
 * D-PMS 사용자 테이블(users)과 Spring Security 구성에 맞춰 만든 것이다.
 */
public interface CompanySignupService {

    /**
     * 입력 규칙을 모두 확인한 뒤 기업회원을 승인 대기 상태로 저장한다.
     *
     * @return 가입한 아이디
     * @throws kr.go.daegu.pms.cmm.exception.FieldBusinessException 입력칸별 규칙 위반 (모든 위반을 한 번에)
     * @throws kr.go.daegu.pms.cmm.exception.BusinessException      로그인한 상태이거나, 확인 직후 같은 아이디·번호로 먼저 가입된 경우
     */
    String insertCompanyMember(CompanySignupVO signup);
}
