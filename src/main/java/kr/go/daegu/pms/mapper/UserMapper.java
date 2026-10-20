package kr.go.daegu.pms.mapper;

import kr.go.daegu.pms.domain.User;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Optional;

@Mapper
public interface UserMapper {
    Optional<User> findByUserId(@Param("userId") String userId);
    List<User> findByRole(@Param("role") String role);
    int insertUser(User user);
    int updateUser(User user);
}
