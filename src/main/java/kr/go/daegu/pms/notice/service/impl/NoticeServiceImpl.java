package kr.go.daegu.pms.notice.service.impl;

import jakarta.annotation.Resource;
import kr.go.daegu.pms.cmm.exception.NotFoundException;
import kr.go.daegu.pms.cmm.message.AppMessages;
import kr.go.daegu.pms.cmm.util.SecurityUtil;
import kr.go.daegu.pms.notice.service.NoticeService;
import kr.go.daegu.pms.notice.service.NoticeVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.egovframe.rte.fdl.cmmn.EgovAbstractServiceImpl;
import org.egovframe.rte.fdl.cmmn.exception.BaseRuntimeException;
import org.egovframe.rte.fdl.cmmn.exception.FdlException;
import org.egovframe.rte.fdl.idgnr.EgovIdGnrService;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 공지사항 서비스 구현 (eGovFrame CRUD 코드 생성기 결과를 바탕으로 수정).
 *
 * 생성기 원본 대비 보완 사항 (원본: docs/egov-codegen/notice-generated)
 *  1. 쓰기 작업에 권한 검사 추가 (원본은 누구나 등록/수정/삭제 가능)
 *  2. 상세 조회 결과가 없으면 NotFoundException (원본은 null 반환 → NPE)
 *  3. 작성자/조회수/사용여부는 화면 값이 아니라 서버가 결정
 *  4. 물리 삭제 → 논리 삭제
 */
@Slf4j
@Service("noticeService")
@RequiredArgsConstructor
public class NoticeServiceImpl extends EgovAbstractServiceImpl implements NoticeService {

    private final NoticeMapper noticeMapper;
    private final AppMessages messages;

    /** 공지 ID 채번 (EgovConfigIdGeneration 의 noticeIdGnrService). 생성기 템플릿과 같은 eGov 방식 */
    @Resource(name = "noticeIdGnrService")
    private EgovIdGnrService noticeIdGnrService;

    @Override
    public List<NoticeVO> selectNoticeList(NoticeVO searchVO) {
        return noticeMapper.selectNoticeList(searchVO);
    }

    @Override
    public int selectNoticeListTotCnt(NoticeVO searchVO) {
        return noticeMapper.selectNoticeListTotCnt(searchVO);
    }

    @Override
    public NoticeVO selectNotice(String noticeId) {
        return noticeMapper.selectNotice(noticeId)
                .orElseThrow(() -> messages.notFound("notice.notFound"));
    }

    @Override
    public void updateNoticeViewCount(String noticeId) {
        noticeMapper.updateNoticeViewCount(noticeId);
    }

    @Override
    public String insertNotice(NoticeVO notice) {
        requireStaff();
        try {
            notice.setNoticeId(noticeIdGnrService.getNextStringId());
        } catch (FdlException e) {
            throw new BaseRuntimeException("공지 ID 채번에 실패했습니다.", e);
        }
        notice.setWriterId(SecurityUtil.getCurrentUserId());
        notice.setImportantYn(normalizeYn(notice.getImportantYn()));
        noticeMapper.insertNotice(notice);
        log.info("[공지 등록] noticeId={}, by={}", notice.getNoticeId(), notice.getWriterId());
        return notice.getNoticeId();
    }

    @Override
    public void updateNotice(NoticeVO notice) {
        requireStaff();
        selectNotice(notice.getNoticeId());   // 존재·삭제 여부 확인
        notice.setImportantYn(normalizeYn(notice.getImportantYn()));
        notice.setUpdatedBy(SecurityUtil.getCurrentUserId());
        noticeMapper.updateNotice(notice);
        log.info("[공지 수정] noticeId={}, by={}", notice.getNoticeId(), notice.getUpdatedBy());
    }

    @Override
    public void deleteNotice(String noticeId) {
        requireStaff();
        if (noticeMapper.deleteNotice(noticeId, SecurityUtil.getCurrentUserId()) == 0) {
            throw messages.notFound("notice.delete.notFound");
        }
        log.info("[공지 삭제] noticeId={}, by={}", noticeId, SecurityUtil.getCurrentUserId());
    }

    private static String normalizeYn(String value) {
        return NoticeVO.YES.equals(value) ? NoticeVO.YES : NoticeVO.NO;
    }

    private void requireStaff() {
        if (!SecurityUtil.isStaff()) {
            throw messages.denied("notice.write.denied");
        }
    }
}
