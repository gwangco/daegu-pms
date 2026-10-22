package kr.go.daegu.pms.user.service.impl;

import kr.go.daegu.pms.user.service.UserVO;
import org.apache.ibatis.annotations.Param;
import org.egovframe.rte.psl.dataaccess.mapper.EgovMapper;

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
}
