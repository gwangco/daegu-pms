package kr.go.daegu.pms.settlement.web;

import jakarta.validation.Valid;
import kr.go.daegu.pms.application.service.ApplicationVO;
import kr.go.daegu.pms.cmm.exception.BusinessException;
import kr.go.daegu.pms.cmm.message.AppMessages;
import kr.go.daegu.pms.cmm.pagination.PagingHelper;
import kr.go.daegu.pms.cmm.service.CommonCodeService;
import kr.go.daegu.pms.settlement.service.SettlementSearchVO;
import kr.go.daegu.pms.settlement.service.SettlementService;
import kr.go.daegu.pms.settlement.service.SettlementVO;
import lombok.RequiredArgsConstructor;
import org.egovframe.rte.ptl.mvc.tags.ui.pagination.PaginationInfo;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.Clock;
import java.time.LocalDate;
import java.util.List;

@Controller
@RequestMapping("/settlements")
@RequiredArgsConstructor
public class SettlementController {

    private final SettlementService settlementService;
    private final CommonCodeService commonCodeService;
    private final PagingHelper pagingHelper;
    private final Clock clock;
    private final AppMessages messages;

    // 특정 협약 과제의 사업비 정산 내역 조회 (담당자 또는 과제 수행 기업 본인)
    @GetMapping("/application/{applicationId}")
    public String settlementList(@PathVariable("applicationId") Long applicationId,
                                 @ModelAttribute("searchVO") SettlementSearchVO searchVO, Model model) {
        ApplicationVO application = settlementService.selectSettlementApplication(applicationId);
        long approved = settlementService.selectApprovedAmount(applicationId);
        long used = settlementService.selectUsedAmount(applicationId);

        // 조회 대상 과제는 URL 경로로만 정한다 (검색 파라미터로 다른 과제를 지정할 수 없음)
        searchVO.setApplicationId(applicationId);
        PaginationInfo paginationInfo = pagingHelper.prepare(searchVO);
        List<SettlementVO> settlements = settlementService.selectSettlementList(searchVO);
        paginationInfo.setTotalRecordCount(settlementService.selectSettlementListTotCnt(searchVO));

        model.addAttribute("app", application);
        model.addAttribute("settlements", settlements);
        model.addAttribute("histories", settlementService.selectSettlementHistoryMap(applicationId, settlements));
        model.addAttribute("paginationInfo", paginationInfo);
        model.addAttribute("totalApproved", approved);
        model.addAttribute("totalPending", used - approved);
        model.addAttribute("remaining", application.getRequestGrant() - used);
        model.addAttribute("expItems", commonCodeService.selectCodeList(CommonCodeService.GROUP_EXP_ITEM));
        model.addAttribute("today", LocalDate.now(clock));
        return "settlement/list";
    }

    // 담당자 정산 검수함: 전체 과제의 검수 요청 (기본은 검수 요청 건만)
    @GetMapping("/review")
    @PreAuthorize("hasAnyRole('MANAGER', 'ADMIN')")
    public String reviewList(@ModelAttribute("searchVO") SettlementSearchVO searchVO, Model model) {
        if (searchVO.getStatus() == null) {
            searchVO.setStatus(SettlementVO.STATUS_REQUESTED);
        }
        PaginationInfo paginationInfo = pagingHelper.prepare(searchVO);
        model.addAttribute("settlements", settlementService.selectSettlementReviewList(searchVO));
        paginationInfo.setTotalRecordCount(settlementService.selectSettlementReviewListTotCnt(searchVO));
        model.addAttribute("paginationInfo", paginationInfo);
        return "settlement/review";
    }

    // 사업비 집행 등록 (기업) - 증빙 파일 첨부 필수
    @PostMapping("/register")
    @PreAuthorize("hasRole('COMPANY')")
    public String registerExpense(@Valid @ModelAttribute SettlementVO settlement,
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
            redirectAttributes.addFlashAttribute("successMessage", messages.text("settlement.created"));
        } catch (BusinessException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return redirect;
    }

    // 반려(보완 요청)된 정산 건을 고쳐서 다시 검수 요청 (기업)
    @PostMapping("/{settlementId}/resubmit")
    @PreAuthorize("hasRole('COMPANY')")
    public String resubmit(@PathVariable("settlementId") Long settlementId,
                           @Valid @ModelAttribute SettlementVO input,
                           BindingResult bindingResult,
                           @RequestParam(value = "receiptFile", required = false) MultipartFile receiptFile,
                           RedirectAttributes redirectAttributes) {
        Long targetApplicationId = input.getApplicationId();
        if (bindingResult.hasErrors()) {
            redirectAttributes.addFlashAttribute("errorMessage", firstErrorMessage(bindingResult));
        } else {
            try {
                // 이동할 과제도 화면 값이 아니라 실제 정산 건의 과제 ID 를 쓴다
                targetApplicationId = settlementService.updateSettlementResubmit(settlementId, input, receiptFile);
                redirectAttributes.addFlashAttribute("successMessage", messages.text("settlement.resubmitted"));
            } catch (BusinessException e) {
                redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
            }
        }
        return targetApplicationId == null ? "redirect:/applications" : "redirect:/settlements/application/" + targetApplicationId;
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
            redirectAttributes.addFlashAttribute("successMessage", messages.text("settlement.reviewed"));
        } catch (BusinessException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/settlements/application/" + targetApplicationId;
    }

    private String firstErrorMessage(BindingResult bindingResult) {
        return bindingResult.getAllErrors().stream()
                .map(error -> error.getDefaultMessage())
                .findFirst()
                .orElseGet(() -> messages.text("common.input.invalid"));
    }
}
