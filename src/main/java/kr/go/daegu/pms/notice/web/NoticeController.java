package kr.go.daegu.pms.notice.web;

import egovframework.com.cop.bbs.service.BoardVO;
import jakarta.validation.Valid;
import kr.go.daegu.pms.cmm.exception.BusinessException;
import kr.go.daegu.pms.cmm.message.AppMessages;
import kr.go.daegu.pms.cmm.pagination.PagingHelper;
import kr.go.daegu.pms.cmm.service.FileService;
import kr.go.daegu.pms.cmm.web.FileController;
import kr.go.daegu.pms.notice.service.NoticeService;
import kr.go.daegu.pms.notice.service.NoticeVO;
import lombok.RequiredArgsConstructor;
import org.egovframe.rte.ptl.mvc.tags.ui.pagination.PaginationInfo;
import org.springframework.core.io.Resource;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;
import java.util.Map;

/**
 * 공지사항 컨트롤러 (전자정부 공통컴포넌트 게시판 cop.bbs 사용).
 *
 * 목록 조회는 eGov 게시판 샘플 컨트롤러와 같은 순서로 쓴다:
 *   PaginationInfo 준비 → selectArticleList(resultList, resultCnt) → 총 건수 설정 → 화면
 * 공통컴포넌트 원본 컨트롤러(EgovArticleController, JSP·*.do)는 가져오지 않고, D-PMS 규칙대로 작성했다.
 *  - URL: /notices, /notices/{게시물 ID} (숫자만 허용, 다른 값은 404)
 *  - 등록/수정/삭제는 @PreAuthorize 로 담당자/관리자만 (서비스에서도 한 번 더 검사)
 *  - 수정·삭제 대상은 URL 의 ID 로만 정한다
 *  - 첨부 다운로드: /notices/{게시물 ID}/files/{순번}. 공지처럼 누구나 받을 수 있고, 삭제된 공지의 파일은 404
 */
@Controller
@RequestMapping("/notices")
@RequiredArgsConstructor
public class NoticeController {

    private final NoticeService noticeService;
    private final FileService fileService;
    private final PagingHelper pagingHelper;
    private final AppMessages messages;

    @GetMapping
    public String selectNoticeList(@ModelAttribute("searchVO") BoardVO searchVO, Model model) {
        PaginationInfo paginationInfo = pagingHelper.prepare(searchVO);

        Map<String, Object> map = noticeService.selectNoticeList(searchVO);
        int totCnt = Integer.parseInt((String) map.get("resultCnt"));
        paginationInfo.setTotalRecordCount(totCnt);

        model.addAttribute("resultList", map.get("resultList"));
        model.addAttribute("paginationInfo", paginationInfo);
        return "notice/list";
    }

    @GetMapping("/{nttId:\\d+}")
    public String selectNotice(@PathVariable("nttId") long nttId, Model model) {
        BoardVO notice = noticeService.selectNotice(nttId);   // 없으면 404
        noticeService.updateNoticeViewCount(nttId);
        notice.setInqireCo(notice.getInqireCo() + 1);
        model.addAttribute("notice", notice);
        model.addAttribute("files", noticeService.selectNoticeFiles(notice));
        return "notice/detail";
    }

    @GetMapping("/{nttId:\\d+}/files/{fileSn:\\d{1,5}}")
    public ResponseEntity<Resource> downloadFile(@PathVariable("nttId") long nttId, @PathVariable("fileSn") int fileSn) {
        return FileController.attachment(fileService.loadNoticeFile(nttId, fileSn));
    }

    @GetMapping("/new")
    @PreAuthorize("hasAnyRole('MANAGER', 'ADMIN')")
    public String addNoticeView(Model model) {
        NoticeVO notice = new NoticeVO();
        notice.setNoticeAt(NoticeVO.NO);
        model.addAttribute("notice", notice);
        model.addAttribute("files", List.of());
        model.addAttribute("policy", noticeService.selectAttachPolicy());
        return "notice/form";
    }

    @PostMapping("/new")
    @PreAuthorize("hasAnyRole('MANAGER', 'ADMIN')")
    public String addNotice(@Valid @ModelAttribute("notice") NoticeVO notice, BindingResult bindingResult,
                            @RequestParam(value = "files", required = false) List<MultipartFile> files,
                            Model model, RedirectAttributes redirectAttributes) {
        if (!bindingResult.hasErrors()) {
            try {
                long nttId = noticeService.insertNotice(notice, files);
                redirectAttributes.addFlashAttribute("successMessage", messages.text("notice.created"));
                return "redirect:/notices/" + nttId;
            } catch (BusinessException e) {
                // 첨부 개수·크기·형식 오류는 입력한 제목·내용을 그대로 둔 채 같은 화면에 보여 준다
                bindingResult.reject("business", e.getMessage());
            }
        }
        model.addAttribute("files", List.of());
        model.addAttribute("policy", noticeService.selectAttachPolicy());
        return "notice/form";
    }

    @GetMapping("/{nttId:\\d+}/edit")
    @PreAuthorize("hasAnyRole('MANAGER', 'ADMIN')")
    public String updateNoticeView(@PathVariable("nttId") long nttId, Model model) {
        BoardVO notice = noticeService.selectNotice(nttId);
        model.addAttribute("notice", notice);
        model.addAttribute("files", noticeService.selectNoticeFiles(notice));
        model.addAttribute("policy", noticeService.selectAttachPolicy());
        return "notice/form";
    }

    @PostMapping("/{nttId:\\d+}/edit")
    @PreAuthorize("hasAnyRole('MANAGER', 'ADMIN')")
    public String updateNotice(@PathVariable("nttId") long nttId,
                               @Valid @ModelAttribute("notice") NoticeVO notice, BindingResult bindingResult,
                               @RequestParam(value = "files", required = false) List<MultipartFile> files,
                               @RequestParam(value = "deleteFileSn", required = false) List<Integer> deleteFileSns,
                               Model model, RedirectAttributes redirectAttributes) {
        notice.setNttId(nttId);   // 수정 대상은 URL 의 ID 로만 결정 (폼 값으로 다른 글을 수정하는 것 방지)
        if (!bindingResult.hasErrors()) {
            try {
                noticeService.updateNotice(notice, files, deleteFileSns);
                redirectAttributes.addFlashAttribute("successMessage", messages.text("notice.updated"));
                return "redirect:/notices/" + nttId;
            } catch (BusinessException e) {
                bindingResult.reject("business", e.getMessage());
            }
        }
        model.addAttribute("files", noticeService.selectNoticeFiles(noticeService.selectNotice(nttId)));
        model.addAttribute("policy", noticeService.selectAttachPolicy());
        return "notice/form";
    }

    @PostMapping("/{nttId:\\d+}/delete")
    @PreAuthorize("hasAnyRole('MANAGER', 'ADMIN')")
    public String deleteNotice(@PathVariable("nttId") long nttId, RedirectAttributes redirectAttributes) {
        noticeService.deleteNotice(nttId);
        redirectAttributes.addFlashAttribute("successMessage", messages.text("notice.deleted"));
        return "redirect:/notices";
    }
}
