package kr.go.daegu.pms.user.service;

import java.util.List;

/** 사용자 관리 (총괄관리자) */
public interface UserManageService {

    /** 사용자 목록. 연락처·이메일은 마스킹해서 돌려준다 */
    List<UserVO> selectUserList(UserSearchVO searchVO);

    int selectUserListTotCnt(UserSearchVO searchVO);

    /** 로그인 실패 잠금 해제 */
    void updateUserUnlock(String userId);

    /** 로그인한 본인 정보 (연락처·이메일 복호화, 마스킹하지 않음) */
    UserVO selectMyInfo();

    /** 평문으로 남은 연락처·이메일을 eGov ARIA 암호문으로 전환하고 전환한 사용자 수를 돌려준다 (시스템 작업) */
    int updatePlainContactsToEncrypted();
}
