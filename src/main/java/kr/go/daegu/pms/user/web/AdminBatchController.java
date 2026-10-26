package kr.go.daegu.pms.user.web;

import kr.go.daegu.pms.announcement.batch.AnnouncementCloseScheduler;
import kr.go.daegu.pms.cmm.message.AppMessages;
import kr.go.daegu.pms.cmm.service.BatchExecutionVO;
import kr.go.daegu.pms.cmm.service.BatchHistoryService;
import kr.go.daegu.pms.cmm.util.SecurityUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.StepExecution;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * 시스템 관리: 배치 실행 이력과 수동 실행 (총괄관리자).
 * 수동 실행은 트랜잭션 서비스를 거치지 않고 여기서 바로 잡을 실행한다 (트랜잭션 안에서 실행하면 Spring Batch 가 거부)
 */
@Slf4j
@Controller
@RequestMapping("/admin/batch")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
public class AdminBatchController {

    private static final int HISTORY_SIZE = 20;

    private final BatchHistoryService batchHistoryService;
    private final AnnouncementCloseScheduler announcementCloseScheduler;
    private final AppMessages messages;

    @GetMapping
    public String history(Model model) {
        model.addAttribute("executions", batchHistoryService.selectBatchExecutionList(HISTORY_SIZE));
        return "admin/batch";
    }

    @PostMapping("/announcement-close")
    public String runAnnouncementClose(RedirectAttributes redirectAttributes) {
        try {
            JobExecution execution = announcementCloseScheduler.runNow(SecurityUtil.getCurrentUserId());
            long closed = execution.getStepExecutions().stream().mapToLong(StepExecution::getWriteCount).sum();
            redirectAttributes.addFlashAttribute("successMessage", messages.text("batch.announcementClose.done",
                    execution.getId(), BatchExecutionVO.statusLabel(execution.getStatus().name()), closed));
        } catch (Exception e) {
            log.error("[배치] 공고 마감 수동 실행 실패", e);
            redirectAttributes.addFlashAttribute("errorMessage", messages.text("batch.run.failed"));
        }
        return "redirect:/admin/batch";
    }
}
