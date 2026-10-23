package kr.go.daegu.pms.cmm.service.impl;

import kr.go.daegu.pms.cmm.service.PrivacyAccessLogVO;
import kr.go.daegu.pms.cmm.service.PrivacyAccessSearchVO;
import org.egovframe.rte.psl.dataaccess.mapper.EgovMapper;

import java.util.List;

@EgovMapper
public interface PrivacyAccessLogMapper {

    int insertPrivacyAccess(PrivacyAccessLogVO log);

    List<PrivacyAccessLogVO> selectPrivacyAccessList(PrivacyAccessSearchVO searchVO);

    int selectPrivacyAccessListTotCnt(PrivacyAccessSearchVO searchVO);
}
