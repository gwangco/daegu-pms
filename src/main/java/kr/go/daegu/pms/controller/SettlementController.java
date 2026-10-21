package kr.go.daegu.pms.controller;

import jakarta.validation.Valid;
import kr.go.daegu.pms.common.exception.BusinessException;
import kr.go.daegu.pms.domain.Application;
import kr.go.daegu.pms.domain.Settlement;
import kr.go.daegu.pms.service.CommonCodeService;
import kr.go.daegu.pms.service.SettlementService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.Clock;
import java.time.LocalDate;

@Controller
@RequestMapping("/settlements")
@RequiredArgsConstructor
public class SettlementController {

    private final SettlementService settlementService;
    private final CommonCodeService commonCodeService;
    private final Clock clock;

    // 특정 협약 과제의 사업비 정산 내역 조회 (담당자 또는 과제 수행 기업 본인)
    @GetMapping("/application/{applicationId}")
    public String settlementList(@PathVariable("applicationId") Long applicationId, Model model) {
        Application application = settlementService.selectSettlementApplication(applicationId);
        long approved = settlementService.selectApprovedAmount(applicationId);
        long used = settlementService.selectUsedAmount(applicationId);

        model.addAttribute("app", application);
        model.addAttribute("settlements", settlementService.selectSettlementList(applicationId));
        model.addAttribute("totalApproved", approved);
        model.addAttribute("totalPending", used - approved);
        model.addAttribute("remaining", application.getRequestGrant() - used);
        model.addAttribute("expItems", commonCodeService.selectCodeList(CommonCodeService.GROUP_EXP_ITEM));
        model.addAttribute("today", LocalDate.now(clock));
        return "settlement/list";
    }

    // 사업비 집행 등록 (기업) - 증빙 파일 첨부 필수
    @PostMapping("/register")
    @PreAuthorize("hasRole('COMPANY')")
    public String registerExpense(@Valid @ModelAttribute Settlement settlement,
                                  BindingResult bindingResult,
                                  @RequestParam(value = "receiptFile", required = false) MultipartFile receiptFile,
                                  RedirectAttributes redirectAttributes) {
        if (settlement.getApplicationId() == null) {
            return "redirect:/applications";
        }
        String redirect = "redirect:/settlements/application/" + settlement.getApplicationId();

        if (bindingResult.hasErrors()) {
            redirectAttributes.addFlashAttribute("errorMessage", firstErrorMessage(bindingResult));
            return redirect;
        }
        try {
            settlementService.insertSettlement(settlement, receiptFile);
            redirectAttributes.addFlashAttribute("successMessage", "사업비 집행 내역이 등록되었습니다. 담당자 검수 후 승인됩니다.");
        } catch (BusinessException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return redirect;
    }

    // 사업비 검수 승인 / 반려 (담당자/관리자)
    @PostMapping("/{settlementId}/review")
    @PreAuthorize("hasAnyRole('MANAGER', 'ADMIN')")
    public String reviewSettlement(@PathVariable("settlementId") Long settlementId,
                                   @RequestParam("applicationId") Long applicationId,
                                   @RequestParam("status") String status,
                                   @RequestParam(value = "rejectReason", required = false) String rejectReason,
                                   RedirectAttributes redirectAttributes) {
        Long targetApplicationId = applicationId;
        try {
            // 리다이렉트 대상도 화면 값이 아니라 실제 정산 건의 과제 ID 를 사용
            targetApplicationId = settlementService.reviewSettlement(settlementId, status, rejectReason);
            redirectAttributes.addFlashAttribute("successMessage", "검수 처리가 완료되었습니다.");
        } catch (BusinessException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/settlements/application/" + targetApplicationId;
    }

    private String firstErrorMessage(BindingResult bindingResult) {
        return bindingResult.getAllErrors().stream()
                .map(error -> error.getDefaultMessage())
                .findFirst()
                .orElse("입력값을 확인하세요.");
    }
}
