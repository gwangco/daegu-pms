package kr.go.daegu.pms.user.service.impl;

import kr.go.daegu.pms.cmm.crypto.PersonalInfoCipher;
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

/**
 * 사용자 관리.
 * 연락처·이메일은 DB 에 eGov ARIA 암호문으로 저장되어 있고, 화면에 줄 때만 복호화한다.
 * 관리자 목록에는 복호화한 뒤 가린 값만 내려주고, 원본은 본인 정보 화면에서만 보인다.
 */
@Slf4j
@Service("userManageService")
@RequiredArgsConstructor
public class UserManageServiceImpl extends EgovAbstractServiceImpl implements UserManageService {

    private final UserMapper userMapper;
    private final LoginLockPolicy lockPolicy;
    private final PersonalInfoCipher cipher;
    private final AppMessages messages;

    @Override
    public List<UserVO> selectUserList(UserSearchVO searchVO) {
        requireAdmin();
        searchVO.setLockedSince(lockPolicy.activeLockSince());
        // 조회 결과(MyBatis 1차 캐시 객체)를 고치지 않고 화면용 복사본을 만든다
        return userMapper.selectUserList(searchVO).stream()
                .map(user -> user.toBuilder()
                        .phone(MaskingUtil.phone(cipher.decrypt(user.getPhone())))
                        .email(MaskingUtil.email(cipher.decrypt(user.getEmail())))
                        // 잠금 시간이 지난 기록은 화면에서 잠김으로 보이지 않게 정리
                        .lockedAt(lockPolicy.isLocked(user.getLockedAt()) ? user.getLockedAt() : null)
                        .build())
                .toList();
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

    @Override
    public UserVO selectMyInfo() {
        String userId = SecurityUtil.getCurrentUserId();
        if (userId == null) {
            throw messages.denied("common.login.required");
        }
        UserVO user = userMapper.selectUser(userId).orElseThrow(() -> messages.notFound("user.notFound"));
        return user.toBuilder()
                .phone(cipher.decrypt(user.getPhone()))
                .email(cipher.decrypt(user.getEmail()))
                .build();
    }

    @Override
    public int updatePlainContactsToEncrypted() {
        List<UserVO> plain = userMapper.selectPlainContactList(PersonalInfoCipher.PREFIX);
        plain.forEach(user -> userMapper.updateUserContact(user.getUserId(),
                cipher.encrypt(user.getEmail()), cipher.encrypt(user.getPhone())));
        if (!plain.isEmpty()) {
            log.info("[개인정보 암호화 전환] 연락처·이메일 {}명", plain.size());
        }
        return plain.size();
    }

    private void requireAdmin() {
        if (!SecurityUtil.hasRole(SecurityUtil.ROLE_ADMIN)) {
            throw messages.denied("admin.denied");
        }
    }
}
