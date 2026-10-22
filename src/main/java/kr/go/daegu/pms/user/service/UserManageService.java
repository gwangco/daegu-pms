package kr.go.daegu.pms.user.service;

import java.util.List;

/** 사용자 관리 (총괄관리자) */
public interface UserManageService {

    /** 사용자 목록. 연락처·이메일은 마스킹해서 돌려준다 */
    List<UserVO> selectUserList(UserSearchVO searchVO);

    int selectUserListTotCnt(UserSearchVO searchVO);

    /** 로그인 실패 잠금 해제 */
    void updateUserUnlock(String userId);
}
