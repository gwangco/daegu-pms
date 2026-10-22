package kr.go.daegu.pms.user.service.impl;

import kr.go.daegu.pms.user.service.UserSearchVO;
import kr.go.daegu.pms.user.service.UserVO;
import org.apache.ibatis.annotations.Param;
import org.egovframe.rte.psl.dataaccess.mapper.EgovMapper;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@EgovMapper
public interface UserMapper {

    /** 로그인 인증용. 비밀번호 해시를 함께 조회하므로 인증 처리 외에는 쓰지 않는다 */
    Optional<UserVO> selectUserForLogin(@Param("userId") String userId);

    /** 사용자 1명 (비밀번호 제외) */
    Optional<UserVO> selectUser(@Param("userId") String userId);

    /** 권한별 사용자 목록 (비밀번호 제외) */
    List<UserVO> selectUserListByRole(@Param("role") String role);

    /** 로그인 실패 횟수 계산용 행 잠금 조회 */
    Optional<UserVO> selectUserForUpdate(@Param("userId") String userId);

    int updateLoginFailure(@Param("userId") String userId,
                           @Param("loginFailCount") int loginFailCount,
                           @Param("lockedAt") LocalDateTime lockedAt);

    int updateLoginSuccess(@Param("userId") String userId);

    /** 잠긴 계정만 해제된다. 반환값 0 이면 잠겨 있지 않은 계정 */
    int updateUserUnlock(@Param("userId") String userId);

    List<UserVO> selectUserList(UserSearchVO searchVO);

    int selectUserListTotCnt(UserSearchVO searchVO);
}
