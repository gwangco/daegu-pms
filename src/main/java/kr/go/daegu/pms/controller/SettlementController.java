package kr.go.daegu.pms.controller;

import kr.go.daegu.pms.domain.Application;
import kr.go.daegu.pms.domain.Settlement;
import kr.go.daegu.pms.service.ApplicationService;
import kr.go.daegu.pms.service.SettlementService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Controller
@RequestMapping("/settlements")
@RequiredArgsConstructor
public class SettlementController {

    private final SettlementService settlementService;
    private final ApplicationService applicationService;

    // 특정 협약 과제의 사업비 정산 내역 조회
    @GetMapping("/application/{applicationId}")
    public String settlementList(@PathVariable("applicationId") Long applicationId, Model model) {
        Application application = applicationService.getApplicationById(applicationId);
        List<Settlement> settlements = settlementService.getSettlementsByApplication(applicationId);
        Long totalApproved = settlementService.getTotalApprovedAmount(applicationId);

        model.addAttribute("application", application);
        model.addAttribute("settlements", settlements);
        model.addAttribute("totalApproved", totalApproved);
        model.addAttribute("newSettlement", Settlement.builder().applicationId(applicationId).build());
        return "settlement/list";
    }

    // 사업비 집행 등록 (기업)
    @PostMapping("/register")
    @PreAuthorize("hasRole('COMPANY')")
    public String registerExpense(@ModelAttribute Settlement settlement) {
        settlementService.registerExpense(settlement);
        return "redirect:/settlements/application/" + settlement.getApplicationId();
    }

    // 사업비 검수 승인 / 반려 (담당자/관리자)
    @PostMapping("/{settlementId}/review")
    @PreAuthorize("hasAnyRole('MANAGER', 'ADMIN')")
    public String reviewSettlement(
            @PathVariable("settlementId") Long settlementId,
            @RequestParam("applicationId") Long applicationId,
            @RequestParam("status") String status,
            @RequestParam(value = "rejectReason", required = false) String rejectReason) {

        settlementService.approveOrReject(settlementId, status, rejectReason);
        return "redirect:/settlements/application/" + applicationId;
    }
}
