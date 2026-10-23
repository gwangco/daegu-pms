package kr.go.daegu.pms.notice.service.impl;

import egovframework.com.cop.bbs.service.Board;
import egovframework.com.cop.bbs.service.BoardVO;
import egovframework.com.cop.bbs.service.EgovArticleService;
import kr.go.daegu.pms.cmm.message.AppMessages;
import kr.go.daegu.pms.cmm.service.AttachedFile;
import kr.go.daegu.pms.cmm.service.FileService;
import kr.go.daegu.pms.cmm.util.SecurityUtil;
import kr.go.daegu.pms.notice.service.NoticeAttachPolicyVO;
import kr.go.daegu.pms.notice.service.NoticeService;
import kr.go.daegu.pms.notice.service.NoticeVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.egovframe.rte.fdl.cmmn.EgovAbstractServiceImpl;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 공지사항 서비스 구현.
 *
 * 공통컴포넌트 게시판(EgovArticleService)을 호출하기 전에 D-PMS 업무 규칙을 적용한다.
 *  1. 게시판 ID 는 항상 공지사항 게시판으로 고정 (검색 파라미터로 다른 게시판 글을 조회·수정하는 것 방지)
 *  2. 등록·수정·삭제는 담당자/관리자만 (컨트롤러 @PreAuthorize 와 별도로 서비스에서도 검사)
 *  3. 화면 값 중 제목·내용·중요여부만 옮겨 담는다 (작성자·조회수·비밀글 같은 컬럼은 서버가 정함)
 *  4. 없거나 이미 삭제된 글은 NotFoundException (404)
 *  5. 첨부파일 ID 는 화면 값을 쓰지 않는다. 등록은 새로 만든 묶음, 수정은 잠근 게시물 행에서 읽은 묶음만 쓴다
 *     (공통컴포넌트 원본 EgovArticleController 의 IDOR 조치와 같은 원칙)
 *  6. 첨부 개수·크기는 게시판 속성(COMTNBBSMASTER)으로 서버에서 검사 (화면 안내만 믿지 않는다)
 */
@Slf4j
@Service("noticeService")
@RequiredArgsConstructor
public class NoticeServiceImpl extends EgovAbstractServiceImpl implements NoticeService {

    /** 공통컴포넌트 게시판 서비스 */
    private final EgovArticleService articleService;
    private final NoticeMapper noticeMapper;
    private final FileService fileService;
    private final AppMessages messages;

    @Override
    public Map<String, Object> selectNoticeList(BoardVO searchVO) {
        searchVO.setBbsId(BBS_ID);
        return articleService.selectArticleList(searchVO);
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<BoardVO> selectRecentNoticeList(int count) {
        BoardVO searchVO = new BoardVO();
        searchVO.setBbsId(BBS_ID);
        searchVO.setFirstIndex(0);
        searchVO.setRecordCountPerPage(count);
        return (List<BoardVO>) articleService.selectArticleList(searchVO).get("resultList");
    }

    @Override
    public BoardVO selectNotice(long nttId) {
        BoardVO notice = articleService.selectArticleDetailNoCount(key(nttId));
        if (notice == null) {
            throw messages.notFound("notice.notFound");
        }
        return notice;
    }

    @Override
    public List<AttachedFile> selectNoticeFiles(BoardVO notice) {
        return fileService.selectFiles(notice.getAtchFileId());
    }

    @Override
    public NoticeAttachPolicyVO selectAttachPolicy() {
        NoticeAttachPolicyVO policy = noticeMapper.selectAttachPolicy(BBS_ID);
        return policy != null ? policy : new NoticeAttachPolicyVO();   // 게시판 속성이 없으면 첨부 불가로 본다
    }

    @Override
    public void updateNoticeViewCount(long nttId) {
        articleService.increaseInqireCo(key(nttId));
    }

    @Override
    public long insertNotice(NoticeVO notice, List<MultipartFile> files) {
        requireStaff();
        List<MultipartFile> uploads = selected(files);
        NoticeAttachPolicyVO policy = selectAttachPolicy();
        checkAttachLimit(policy, 0, uploads.size());

        Board board = new Board();
        board.setBbsId(BBS_ID);
        board.setNttSj(notice.getNttSj());
        board.setNttCn(notice.getNttCn());
        board.setNoticeAt(normalizeYn(notice.getNoticeAt()));
        // 파일을 먼저 저장하고 묶음 ID 를 게시물에 연결한다. 게시물 저장이 실패하면 트랜잭션 롤백으로 파일도 지워진다
        board.setAtchFileId(uploads.isEmpty() ? null : fileService.storeFiles(uploads, null, policy.getMaxFileSize()));
        articleService.insertArticle(board);
        log.info("[공지 등록] nttId={}, files={}, by={}", board.getNttId(), uploads.size(), SecurityUtil.getCurrentUserId());
        return board.getNttId();
    }

    @Override
    public void updateNotice(NoticeVO notice, List<MultipartFile> files, List<Integer> deleteFileSns) {
        requireStaff();
        // 게시물 행을 먼저 잠그고, 첨부 ID 는 그 행의 값만 쓴다 (화면이 보낸 첨부 ID 로 다른 글의 파일을 건드리지 못하게)
        BoardVO locked = noticeMapper.selectNoticeForUpdate(BBS_ID, notice.getNttId());
        if (locked == null) {
            throw messages.notFound("notice.notFound");
        }
        String atchFileId = StringUtils.hasText(locked.getAtchFileId()) ? locked.getAtchFileId() : null;

        // 지울 순번은 이 공지에 실제로 있는 것만 남긴다
        Set<Integer> current = fileService.selectFiles(atchFileId).stream()
                .map(AttachedFile::fileSn)
                .collect(Collectors.toSet());
        List<Integer> toDelete = deleteFileSns == null ? List.of() : deleteFileSns.stream()
                .filter(Objects::nonNull)
                .filter(current::contains)
                .distinct()
                .toList();
        List<MultipartFile> uploads = selected(files);
        NoticeAttachPolicyVO policy = selectAttachPolicy();
        checkAttachLimit(policy, current.size() - toDelete.size(), uploads.size());

        // 추가를 먼저 한다. 지운 뒤에 추가하면 마지막 순번을 지웠을 때 새 파일이 그 순번을 다시 받아,
        // 예전 다운로드 주소(/notices/{번호}/files/{순번})가 다른 파일을 가리키게 된다
        if (!uploads.isEmpty()) {
            atchFileId = fileService.storeFiles(uploads, atchFileId, policy.getMaxFileSize());
        }
        for (int fileSn : toDelete) {
            fileService.deleteFile(atchFileId, fileSn);
        }

        Board board = new Board();
        board.setBbsId(BBS_ID);
        board.setNttId(notice.getNttId());
        board.setNttSj(notice.getNttSj());
        board.setNttCn(notice.getNttCn());
        board.setNoticeAt(normalizeYn(notice.getNoticeAt()));
        board.setAtchFileId(atchFileId);   // SQL 은 첨부 ID 가 비어 있을 때만 연결한다
        if (articleService.updateArticle(board) == 0) {
            throw messages.notFound("notice.notFound");
        }
        log.info("[공지 수정] nttId={}, deleted={}, added={}, by={}", notice.getNttId(), toDelete.size(), uploads.size(),
                SecurityUtil.getCurrentUserId());
    }

    @Override
    public void deleteNotice(long nttId) {
        requireStaff();
        Board board = new Board();
        board.setBbsId(BBS_ID);
        board.setNttId(nttId);
        if (articleService.deleteArticle(board) == 0) {
            throw messages.notFound("notice.delete.notFound");
        }
        log.info("[공지 삭제] nttId={}, by={}", nttId, SecurityUtil.getCurrentUserId());
    }

    /** 첨부 개수 검사. 새로 올리는 파일이 없으면 검사하지 않는다 (첨부를 막은 뒤에도 기존 파일 삭제·글 수정은 가능) */
    private void checkAttachLimit(NoticeAttachPolicyVO policy, int remaining, int adding) {
        if (adding == 0) {
            return;
        }
        if (!policy.isAllowed()) {
            throw messages.business("notice.file.notAllowed");
        }
        if (remaining + adding > policy.getPosblAtchFileNumber()) {
            throw messages.business("notice.file.count.exceeded", policy.getPosblAtchFileNumber());
        }
    }

    /** 브라우저는 파일을 고르지 않아도 빈 파일 칸을 보내므로 실제로 고른 파일만 남긴다 */
    private static List<MultipartFile> selected(List<MultipartFile> files) {
        return files == null ? List.of() : files.stream().filter(f -> f != null && !f.isEmpty()).toList();
    }

    private static BoardVO key(long nttId) {
        BoardVO key = new BoardVO();
        key.setBbsId(BBS_ID);
        key.setNttId(nttId);
        return key;
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
