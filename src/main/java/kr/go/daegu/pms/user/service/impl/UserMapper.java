package kr.go.daegu.pms.user.service.impl;

import kr.go.daegu.pms.user.service.UserVO;
import org.egovframe.rte.psl.dataaccess.mapper.EgovMapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Optional;

@EgovMapper("userMapper")
public interface UserMapper {
    Optional<UserVO> findByUserId(@Param("userId") String userId);
    List<UserVO> findByRole(@Param("role") String role);
    int insertUser(UserVO user);
    int updateUser(UserVO user);
}
