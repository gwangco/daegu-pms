package kr.go.daegu.pms.user.service;

import lombok.Data;
import lombok.ToString;

/**
 * 기업회원 가입 입력값.
 *
 * 화면에서 받을 값만 둔다. 권한(role)·승인 상태·사용 여부처럼 서버가 정하는 값은 필드 자체가 없어서
 * 요청에 role=ROLE_ADMIN 을 끼워 넣어도 바인딩될 곳이 없다 (대량 할당 방지).
 * 비밀번호 칸 이름이 password 가 아닌 것은 GlobalBindingAdvice 가 password 바인딩을 전역으로 막기 때문이다.
 * 입력 규칙 검사는 서비스(CompanySignupServiceImpl) 한 곳에서 한다.
 */
@Data
public class CompanySignupVO {

    private String userId;

    @ToString.Exclude
    private String newPassword;

    @ToString.Exclude
    private String newPasswordConfirm;

    /** 담당자 이름 */
    private String userName;

    private String companyName;

    private String businessNo;

    private String email;

    private String phone;

    /** 이용약관 동의 (필수) */
    private boolean agreeTerms;

    /** 개인정보 수집·이용 동의 (필수) */
    private boolean agreePrivacy;
}
