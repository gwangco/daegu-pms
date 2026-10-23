package kr.go.daegu.pms.cmm.service;

import java.util.List;

/**
 * 개인정보 처리 기록 (안전성 확보조치 기준의 접속기록).
 *
 * 기록은 컨트롤러가 조회·다운로드를 마친 뒤 부른다. 조회 서비스(select*)는 읽기 전용 트랜잭션이라
 * 그 안에서 INSERT 하면 MySQL 이 거부하기 때문이다 (6차에 공통컴포넌트 조회수로 겪은 것과 같은 규칙).
 */
public interface PrivacyAccessLogService {

    /** 로그인한 사용자가 남의 개인정보를 조회·다운로드한 기록 (처리자·일시는 서버가 정한다) */
    void insertPrivacyAccess(String action, String targetType, String targetId, String subjectId,
                             int recordCount, String clientIp);

    /** 처리 기록 목록 (총괄관리자) */
    List<PrivacyAccessLogVO> selectPrivacyAccessList(PrivacyAccessSearchVO searchVO);

    int selectPrivacyAccessListTotCnt(PrivacyAccessSearchVO searchVO);
}
