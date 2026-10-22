package kr.go.daegu.pms.user.service;

import java.util.List;

/**
 * 로그인 결과 처리와 접속 기록.
 * Spring Security 의 로그인 성공/실패/로그아웃 핸들러가 호출한다.
 * (메서드 이름이 insert* 이므로 eGov AOP 규칙에 따라 쓰기 트랜잭션에서 실행된다)
 */
public interface LoginPolicyService {

    /** 로그인 성공: 실패 횟수·잠금 초기화, 최근 로그인 시각과 접속 기록 저장 */
    void insertLoginSuccess(String userId, String clientIp);

    /**
     * 비밀번호(또는 아이디) 오류. 있는 계정이면 실패 횟수를 올리고 한도에 닿으면 잠근다.
     * @return 이번 실패로 계정이 잠겼으면 true
     */
    boolean insertPasswordFailure(String loginId, String clientIp);

    /** 잠긴 계정·사용 중지 계정의 로그인 시도 기록 (실패 횟수는 올리지 않음) */
    void insertBlockedLogin(String loginId, String clientIp, LoginResult result);

    void insertLogout(String userId, String clientIp);

    /** 접속 기록 목록 (총괄관리자) */
    List<LoginHistoryVO> selectLoginHistoryList(LoginHistorySearchVO searchVO);

    int selectLoginHistoryListTotCnt(LoginHistorySearchVO searchVO);
}
