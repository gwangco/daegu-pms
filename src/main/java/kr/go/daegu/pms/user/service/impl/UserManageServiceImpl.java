package kr.go.daegu.pms.user.service.impl;

import kr.go.daegu.pms.cmm.message.AppMessages;
import kr.go.daegu.pms.cmm.util.MaskingUtil;
import kr.go.daegu.pms.cmm.util.SecurityUtil;
import kr.go.daegu.pms.user.service.LoginLockPolicy;
import kr.go.daegu.pms.user.service.UserManageService;
import kr.go.daegu.pms.user.service.UserSearchVO;
import kr.go.daegu.pms.user.service.UserVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.egovframe.rte.fdl.cmmn.EgovAbstractServiceImpl;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service("userManageService")
@RequiredArgsConstructor
public class UserManageServiceImpl extends EgovAbstractServiceImpl implements UserManageService {

    private final UserMapper userMapper;
    private final LoginLockPolicy lockPolicy;
    private final AppMessages messages;

    @Override
    public List<UserVO> selectUserList(UserSearchVO searchVO) {
        requireAdmin();
        searchVO.setLockedSince(lockPolicy.activeLockSince());
        List<UserVO> users = userMapper.selectUserList(searchVO);
        users.forEach(user -> {
            user.setPhone(MaskingUtil.phone(user.getPhone()));
            user.setEmail(MaskingUtil.email(user.getEmail()));
            // 잠금 시간이 지난 기록은 화면에서 잠김으로 보이지 않게 정리
            if (!lockPolicy.isLocked(user.getLockedAt())) {
                user.setLockedAt(null);
            }
        });
        return users;
    }

    @Override
    public int selectUserListTotCnt(UserSearchVO searchVO) {
        requireAdmin();
        searchVO.setLockedSince(lockPolicy.activeLockSince());
        return userMapper.selectUserListTotCnt(searchVO);
    }

    @Override
    public void updateUserUnlock(String userId) {
        requireAdmin();
        userMapper.selectUser(userId).orElseThrow(() -> messages.notFound("user.notFound"));
        if (userMapper.updateUserUnlock(userId) == 0) {
            throw messages.business("user.unlock.notLocked");
        }
        log.info("[계정 잠금 해제] user={}, by={}", userId, SecurityUtil.getCurrentUserId());
    }

    private void requireAdmin() {
        if (!SecurityUtil.hasRole(SecurityUtil.ROLE_ADMIN)) {
            throw messages.denied("admin.denied");
        }
    }
}
