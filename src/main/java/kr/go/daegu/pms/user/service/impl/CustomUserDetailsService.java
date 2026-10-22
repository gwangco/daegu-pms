package kr.go.daegu.pms.user.service.impl;

import kr.go.daegu.pms.user.service.LoginLockPolicy;
import kr.go.daegu.pms.user.service.UserVO;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Spring Security 인증용 사용자 조회.
 *
 * 잠긴 계정은 accountNonLocked=false 로 돌려준다. 그러면 Spring Security 가 비밀번호를 비교하기 전에
 * LockedException 을 던지므로, 잠긴 상태에서는 비밀번호가 맞았는지 틀렸는지 알려 주지 않는다.
 */
@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private final UserMapper userMapper;
    private final LoginLockPolicy lockPolicy;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        // 아이디 칸에 비밀번호를 잘못 입력하는 경우가 있어 예외 메시지에 입력값을 넣지 않는다
        UserVO user = userMapper.selectUserForLogin(username)
                .orElseThrow(() -> new UsernameNotFoundException("사용자를 찾을 수 없습니다."));

        return User.withUsername(user.getUserId())
                .password(user.getPassword())
                .disabled(!user.isEnabled())
                .accountLocked(lockPolicy.isLocked(user.getLockedAt()))
                .authorities(List.of(new SimpleGrantedAuthority(user.getRole())))
                .build();
    }
}
