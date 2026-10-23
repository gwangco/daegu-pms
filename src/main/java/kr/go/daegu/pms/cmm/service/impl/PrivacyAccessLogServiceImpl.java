package kr.go.daegu.pms.cmm.service.impl;

import kr.go.daegu.pms.cmm.message.AppMessages;
import kr.go.daegu.pms.cmm.service.PrivacyAccessLogService;
import kr.go.daegu.pms.cmm.service.PrivacyAccessLogVO;
import kr.go.daegu.pms.cmm.service.PrivacyAccessSearchVO;
import kr.go.daegu.pms.cmm.util.SecurityUtil;
import lombok.RequiredArgsConstructor;
import org.egovframe.rte.fdl.cmmn.EgovAbstractServiceImpl;
import org.springframework.stereotype.Service;

import java.util.List;

@Service("privacyAccessLogService")
@RequiredArgsConstructor
public class PrivacyAccessLogServiceImpl extends EgovAbstractServiceImpl implements PrivacyAccessLogService {

    private final PrivacyAccessLogMapper privacyAccessLogMapper;
    private final AppMessages messages;

    @Override
    public void insertPrivacyAccess(String action, String targetType, String targetId, String subjectId,
                                    int recordCount, String clientIp) {
        String userId = SecurityUtil.getCurrentUserId();
        if (userId == null) {
            return;   // 비로그인 화면에는 남의 개인정보가 없다
        }
        privacyAccessLogMapper.insertPrivacyAccess(PrivacyAccessLogVO.builder()
                .userId(userId)
                .action(action)
                .targetType(targetType)
                .targetId(targetId)
                .subjectId(subjectId)
                .recordCount(recordCount)
                .clientIp(clientIp)
                .build());
    }

    @Override
    public List<PrivacyAccessLogVO> selectPrivacyAccessList(PrivacyAccessSearchVO searchVO) {
        requireAdmin();
        return privacyAccessLogMapper.selectPrivacyAccessList(searchVO);
    }

    @Override
    public int selectPrivacyAccessListTotCnt(PrivacyAccessSearchVO searchVO) {
        requireAdmin();
        return privacyAccessLogMapper.selectPrivacyAccessListTotCnt(searchVO);
    }

    private void requireAdmin() {
        if (!SecurityUtil.hasRole(SecurityUtil.ROLE_ADMIN)) {
            throw messages.denied("admin.denied");
        }
    }
}
