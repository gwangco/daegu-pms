package kr.go.daegu.pms.application.web;

import jakarta.validation.Valid;
import kr.go.daegu.pms.announcement.service.AnnouncementService;
import kr.go.daegu.pms.announcement.service.AnnouncementVO;
import kr.go.daegu.pms.application.service.ApplicationSearchVO;
import kr.go.daegu.pms.application.service.ApplicationService;
import kr.go.daegu.pms.application.service.ApplicationStatus;
import kr.go.daegu.pms.application.service.ApplicationVO;
import kr.go.daegu.pms.cmm.exception.BusinessException;
import kr.go.daegu.pms.cmm.message.AppMessages;
import kr.go.daegu.pms.cmm.pagination.PagingHelper;
import kr.go.daegu.pms.cmm.util.SecurityUtil;
import kr.go.daegu.pms.evaluation.service.EvaluationService;
import lombok.RequiredArgsConstructor;
import org.egovframe.rte.ptl.mvc.tags.ui.pagination.PaginationInfo;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

@Controller
@RequestMapping("/applications")
@RequiredArgsConstructor
public class ApplicationController {

    private final ApplicationService applicationService;
    private final AnnouncementService announcementService;
    private final EvaluationService evaluationService;
    private final PagingHelper pagingHelper;
    private final AppMessages messages;

    // 기업: 본인 신청 내역 / 평가위원: 배정 과제 / 담당자·관리자: 전체 (범위는 서비스에서 강제)
    @GetMapping
    public String list(@ModelAttribute("searchVO") ApplicationSearchVO searchVO, Model model) {
        PaginationInfo paginationInfo = pagingHelper.prepare(searchVO);

        model.addAttribute("applications", applicationService.selectApplicationList(searchVO));
        paginationInfo.setTotalRecordCount(applicationService.selectApplicationListTotCnt(searchVO));
        model.addAttribute("paginationInfo", paginationInfo);
        // 임시저장은 신청한 기업만 보므로 담당자·평가위원의 상태 검색에는 넣지 않는다
        model.addAttribute("statuses", SecurityUtil.hasRole(SecurityUtil.ROLE_COMPANY)
                ? ApplicationStatus.values()
                : Arrays.stream(ApplicationStatus.values()).filter(s -> !s.isDraft()).toArray(ApplicationStatus[]::new));
        // 평가위원은 정산 정보를 볼 수 없으므로 e-정산 바로가기를 보여주지 않는다 (서비스에서도 거부)
        model.addAttribute("settlementAccessible", !SecurityUtil.hasRole(SecurityUtil.ROLE_EVALUATOR));
        return "application/list";
    }

    // 사업 신청서 작성 화면. 이 공고에 이미 쓴 신청서가 있으면 그쪽으로 보낸다 (한 공고에 신청서 하나)
    @GetMapping("/apply")
    @PreAuthorize("hasRole('COMPANY')")
    public String applyForm(@RequestParam("announcementId") Long announcementId, Model model,
                            RedirectAttributes redirectAttributes) {
        Optional<ApplicationVO> mine = applicationService.selectMyApplicationByAnnouncement(announcementId);
        if (mine.isPresent()) {
            ApplicationVO existing = mine.get();
            if (ApplicationStatus.TEMP_SAVED.name().equals(existing.getStatus())) {
                return "redirect:/applications/" + existing.getApplicationId() + "/edit";
            }
            redirectAttributes.addFlashAttribute("errorMessage", messages.text("application.duplicate"));
            return "redirect:/applications/" + existing.getApplicationId();
        }
        AnnouncementVO announcement = announcementService.selectAnnouncement(announcementId);
        model.addAttribute("announcement", announcement);
        model.addAttribute("app", ApplicationVO.builder().announcementId(announcementId).build());
        return "application/form";
    }

    // 임시저장 (처음 쓰는 신청서). "임시저장" 버튼(name=draft)으로 보낸 요청만 여기로 온다. 필수 항목 검사는 제출할 때
    @PostMapping(value = "/apply", params = "draft")
    @PreAuthorize("hasRole('COMPANY')")
    public String saveDraft(@ModelAttribute("app") ApplicationVO application,
                            BindingResult bindingResult,
                            @RequestParam(value = "planFile", required = false) MultipartFile planFile,
                            Model model,
                            RedirectAttributes redirectAttributes) {
        if (!bindingResult.hasErrors()) {
            try {
                Long id = applicationService.insertApplicationDraft(application, planFile);
                redirectAttributes.addFlashAttribute("successMessage", messages.text("application.draft.saved"));
                return "redirect:/applications/" + id + "/edit";
            } catch (BusinessException e) {
                bindingResult.reject("business", e.getMessage());
            }
        }
        model.addAttribute("announcement", announcementService.selectAnnouncement(application.getAnnouncementId()));
        return "application/form";
    }

    // 사업 신청서 제출 (사업계획서 파일 첨부)
    @PostMapping(value = "/apply", params = "!draft")
    @PreAuthorize("hasRole('COMPANY')")
    public String submit(@Valid @ModelAttribute("app") ApplicationVO application,
                         BindingResult bindingResult,
                         @RequestParam(value = "planFile", required = false) MultipartFile planFile,
                         Model model,
                         RedirectAttributes redirectAttributes) {
        if (!bindingResult.hasErrors()) {
            try {
                Long id = applicationService.insertApplication(application, planFile);
                redirectAttributes.addFlashAttribute("successMessage", messages.text("application.submitted"));
                return "redirect:/applications/" + id;
            } catch (BusinessException e) {
                bindingResult.reject("business", e.getMessage());
            }
        }
        model.addAttribute("announcement", announcementService.selectAnnouncement(application.getAnnouncementId()));
        return "application/form";
    }

    // 임시저장 이어서 작성 (본인·임시저장만)
    @GetMapping("/{id}/edit")
    @PreAuthorize("hasRole('COMPANY')")
    public String editForm(@PathVariable("id") Long id, Model model) {
        ApplicationVO application = applicationService.selectApplicationDraft(id);
        model.addAttribute("app", application);
        model.addAttribute("announcement", announcementService.selectAnnouncement(application.getAnnouncementId()));
        return "application/form";
    }

    // 임시저장 다시 저장
    @PostMapping(value = "/{id}/edit", params = "draft")
    @PreAuthorize("hasRole('COMPANY')")
    public String updateDraft(@PathVariable("id") Long id,
                              @ModelAttribute("app") ApplicationVO application,
                              BindingResult bindingResult,
                              @RequestParam(value = "planFile", required = false) MultipartFile planFile,
                              Model model,
                              RedirectAttributes redirectAttributes) {
        if (!bindingResult.hasErrors()) {
            try {
                applicationService.updateApplicationDraft(id, application, planFile, false);
                redirectAttributes.addFlashAttribute("successMessage", messages.text("application.draft.saved"));
                return "redirect:/applications/" + id + "/edit";
            } catch (BusinessException e) {
                bindingResult.reject("business", e.getMessage());
            }
        }
        return draftFormAgain(id, application, model);
    }

    // 임시저장한 신청서 제출 (필수 항목·금액·사업계획서 확인)
    @PostMapping(value = "/{id}/edit", params = "!draft")
    @PreAuthorize("hasRole('COMPANY')")
    public String submitDraft(@PathVariable("id") Long id,
                              @Valid @ModelAttribute("app") ApplicationVO application,
                              BindingResult bindingResult,
                              @RequestParam(value = "planFile", required = false) MultipartFile planFile,
                              Model model,
                              RedirectAttributes redirectAttributes) {
        if (!bindingResult.hasErrors()) {
            try {
                applicationService.updateApplicationDraft(id, application, planFile, true);
                redirectAttributes.addFlashAttribute("successMessage", messages.text("application.submitted"));
                return "redirect:/applications/" + id;
            } catch (BusinessException e) {
                bindingResult.reject("business", e.getMessage());
            }
        }
        return draftFormAgain(id, application, model);
    }

    /** 오류로 작성 화면을 다시 보일 때: 입력값은 그대로, 공고·저장된 파일은 DB 의 임시저장 신청서에서 */
    private String draftFormAgain(Long id, ApplicationVO application, Model model) {
        ApplicationVO saved = applicationService.selectApplicationDraft(id);
        application.setApplicationId(id);
        application.setAnnouncementId(saved.getAnnouncementId());
        application.setPlanAtchFileId(saved.getPlanAtchFileId());
        application.setPlanFileName(saved.getPlanFileName());
        model.addAttribute("announcement", announcementService.selectAnnouncement(saved.getAnnouncementId()));
        return "application/form";
    }

    // 제출 취소: 서류 검토 전(접수완료)·접수 기간 안이면 임시저장으로 되돌린다
    @PostMapping("/{id}/withdraw")
    @PreAuthorize("hasRole('COMPANY')")
    public String withdraw(@PathVariable("id") Long id, RedirectAttributes redirectAttributes) {
        try {
            applicationService.updateApplicationWithdraw(id);
            redirectAttributes.addFlashAttribute("successMessage", messages.text("application.withdraw.done"));
        } catch (BusinessException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/applications/" + id;
    }

    // 임시저장 삭제
    @PostMapping("/{id}/delete")
    @PreAuthorize("hasRole('COMPANY')")
    public String deleteDraft(@PathVariable("id") Long id, RedirectAttributes redirectAttributes) {
        try {
            applicationService.deleteApplicationDraft(id);
            redirectAttributes.addFlashAttribute("successMessage", messages.text("application.draft.deleted"));
            return "redirect:/applications";
        } catch (BusinessException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
            return "redirect:/applications/" + id;
        }
    }

    // 신청서 상세 조회 (열람 권한은 서비스에서 검사)
    @GetMapping("/{id}")
    public String detail(@PathVariable("id") Long id, Model model) {
        ApplicationVO application = applicationService.selectApplication(id);
        model.addAttribute("app", application);
        model.addAttribute("settlementAccessible", !SecurityUtil.hasRole(SecurityUtil.ROLE_EVALUATOR));
        // 기업 본인 신청서: 임시저장이면 이어서 작성·삭제, 접수완료면 제출 취소를 보여 준다 (서버가 다시 확인)
        model.addAttribute("mine", SecurityUtil.hasRole(SecurityUtil.ROLE_COMPANY)
                && application.getCompanyUserId().equals(SecurityUtil.getCurrentUserId()));

        if (SecurityUtil.isStaff()) {
            ApplicationStatus current = ApplicationStatus.fromCode(application.getStatus()).orElse(null);
            model.addAttribute("nextStatuses", current == null ? List.of() : current.manualNextStatuses());
            model.addAttribute("evaluatorAssignable", current != null && current.isEvaluatorAssignable());
            model.addAttribute("evaluations", evaluationService.selectEvaluationListByApplication(id));
            model.addAttribute("assignableEvaluators", evaluationService.selectAssignableEvaluatorList(id));
        }
        return "application/detail";
    }

    // 담당자: 심사 상태 변경 (서류적격, 선정, 탈락)
    @PostMapping("/{id}/status")
    @PreAuthorize("hasAnyRole('MANAGER', 'ADMIN')")
    public String updateStatus(@PathVariable("id") Long id,
                               @RequestParam("status") String status,
                               RedirectAttributes redirectAttributes) {
        try {
            applicationService.updateApplicationStatus(id, status);
            redirectAttributes.addFlashAttribute("successMessage", messages.text("application.status.updated"));
        } catch (BusinessException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/applications/" + id;
    }

    // 담당자: 평가위원 배정
    @PostMapping("/{id}/evaluators")
    @PreAuthorize("hasAnyRole('MANAGER', 'ADMIN')")
    public String assignEvaluator(@PathVariable("id") Long id,
                                  @RequestParam("evaluatorUserId") String evaluatorUserId,
                                  RedirectAttributes redirectAttributes) {
        try {
            evaluationService.assignEvaluator(id, evaluatorUserId);
            redirectAttributes.addFlashAttribute("successMessage", messages.text("evaluation.assigned"));
        } catch (BusinessException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/applications/" + id;
    }
}
