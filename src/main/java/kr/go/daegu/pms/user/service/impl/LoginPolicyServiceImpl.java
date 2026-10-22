package kr.go.daegu.pms.user.service.impl;

import kr.go.daegu.pms.cmm.message.AppMessages;
import kr.go.daegu.pms.cmm.util.SecurityUtil;
import kr.go.daegu.pms.user.service.LoginHistorySearchVO;
import kr.go.daegu.pms.user.service.LoginHistoryVO;
import kr.go.daegu.pms.user.service.LoginLockPolicy;
import kr.go.daegu.pms.user.service.LoginPolicyService;
import kr.go.daegu.pms.user.service.LoginResult;
import kr.go.daegu.pms.user.service.UserVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.egovframe.rte.fdl.cmmn.EgovAbstractServiceImpl;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * 로그인 결과 처리 (계정 잠금 + 접속 기록).
 *
 * 실패 횟수는 사용자 행을 FOR UPDATE 로 잠근 뒤 Java 에서 계산해 저장한다.
 * 같은 계정에 실패 요청이 동시에 와도 횟수가 덮어써지지 않고, SET 절 평가 순서가 DB 마다 다른 문제도 피한다.
 */
@Slf4j
@Service("loginPolicyService")
@RequiredArgsConstructor
public class LoginPolicyServiceImpl extends EgovAbstractServiceImpl implements LoginPolicyService {

    /** users.user_id 길이. 이보다 긴 입력은 존재할 수 없는 아이디로 본다 */
    private static final int MAX_USER_ID_LENGTH = 50;

    private final UserMapper userMapper;
    private final LoginHistoryMapper loginHistoryMapper;
    private final LoginLockPolicy lockPolicy;
    private final AppMessages messages;

    @Override
    public void insertLoginSuccess(String userId, String clientIp) {
        userMapper.updateLoginSuccess(userId);
        record(userId, LoginResult.SUCCESS, clientIp);
        log.info("[로그인] user={}, ip={}", userId, clientIp);
    }

    @Override
    public boolean insertPasswordFailure(String loginId, String clientIp) {
        Optional<UserVO> found = findUserForUpdate(loginId);
        if (found.isEmpty()) {
            // 아이디 칸에 비밀번호를 넣는 실수가 있으므로 없는 아이디는 입력값을 저장·출력하지 않는다
            record(null, LoginResult.FAIL_UNKNOWN_ID, clientIp);
            log.info("[로그인 실패] 없는 아이디, ip={}", clientIp);
            return false;
        }
        UserVO user = found.get();
        // 잠금 시간이 지난 뒤의 첫 실패는 1회부터 다시 센다
        int previous = lockPolicy.isExpired(user.getLockedAt()) ? 0 : user.getLoginFailCount();
        int failCount = previous + 1;
        boolean lockedNow = failCount >= lockPolicy.getMaxFailures();
        LocalDateTime lockedAt = lockedNow ? lockPolicy.now() : null;

        userMapper.updateLoginFailure(user.getUserId(), failCount, lockedAt);
        record(user.getUserId(), LoginResult.FAIL_PASSWORD, clientIp);
        if (lockedNow) {
            log.warn("[계정 잠금] user={}, 연속 실패 {}회, ip={}", user.getUserId(), failCount, clientIp);
        } else {
            log.info("[로그인 실패] user={}, 연속 실패 {}회, ip={}", user.getUserId(), failCount, clientIp);
        }
        return lockedNow;
    }

    @Override
    public void insertBlockedLogin(String loginId, String clientIp, LoginResult result) {
        String userId = findUserForUpdate(loginId).map(UserVO::getUserId).orElse(null);
        record(userId, result, clientIp);
        log.info("[로그인 차단] user={}, 사유={}, ip={}", userId, result, clientIp);
    }

    @Override
    public void insertLogout(String userId, String clientIp) {
        record(userId, LoginResult.LOGOUT, clientIp);
    }

    @Override
    public List<LoginHistoryVO> selectLoginHistoryList(LoginHistorySearchVO searchVO) {
        requireAdmin();
        return loginHistoryMapper.selectLoginHistoryList(searchVO);
    }

    @Override
    public int selectLoginHistoryListTotCnt(LoginHistorySearchVO searchVO) {
        requireAdmin();
        return loginHistoryMapper.selectLoginHistoryListTotCnt(searchVO);
    }

    private Optional<UserVO> findUserForUpdate(String loginId) {
        if (loginId == null || loginId.isBlank() || loginId.length() > MAX_USER_ID_LENGTH) {
            return Optional.empty();
        }
        return userMapper.selectUserForUpdate(loginId.trim());
    }

    private void record(String userId, LoginResult result, String clientIp) {
        loginHistoryMapper.insertLoginHistory(LoginHistoryVO.builder()
                .userId(userId)
                .resultCode(result.name())
                .clientIp(clientIp)
                .build());
    }

    private void requireAdmin() {
        if (!SecurityUtil.hasRole(SecurityUtil.ROLE_ADMIN)) {
            throw messages.denied("admin.denied");
        }
    }
}
