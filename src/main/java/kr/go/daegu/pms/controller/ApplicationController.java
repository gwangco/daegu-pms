package kr.go.daegu.pms.controller;

import kr.go.daegu.pms.domain.Announcement;
import kr.go.daegu.pms.domain.Application;
import kr.go.daegu.pms.service.AnnouncementService;
import kr.go.daegu.pms.service.ApplicationService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Controller
@RequestMapping("/applications")
@RequiredArgsConstructor
public class ApplicationController {

    private final ApplicationService applicationService;
    private final AnnouncementService announcementService;

    // 기업 사용자: 본인 신청 내역 조회 / 관리자: 전체 신청 내역 조회
    @GetMapping
    public String list(
            @RequestParam(value = "announcementId", required = false) Long announcementId,
            @RequestParam(value = "status", required = false) String status,
            Authentication authentication,
            Model model) {

        String companyUserId = null;
        boolean isCompany = authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_COMPANY"));

        if (isCompany) {
            companyUserId = authentication.getName();
        }

        List<Application> list = applicationService.getApplications(announcementId, companyUserId, status);
        model.addAttribute("applications", list);
        model.addAttribute("isCompany", isCompany);
        return "application/list";
    }

    // 사업 신청서 작성 화면
    @GetMapping("/apply")
    @PreAuthorize("hasRole('COMPANY')")
    public String applyForm(@RequestParam("announcementId") Long announcementId, Model model) {
        Announcement announcement = announcementService.getAnnouncementDetail(announcementId);
        Application application = Application.builder()
                .announcementId(announcementId)
                .announcementTitle(announcement.getTitle())
                .build();

        model.addAttribute("announcement", announcement);
        model.addAttribute("application", application);
        return "application/form";
    }

    // 사업 신청서 제출
    @PostMapping("/apply")
    @PreAuthorize("hasRole('COMPANY')")
    public String submit(
            @ModelAttribute Application application,
            Authentication authentication) {

        application.setCompanyUserId(authentication.getName());
        applicationService.submitApplication(application);
        return "redirect:/applications";
    }

    // 신청서 상세 조회
    @GetMapping("/{id}")
    public String detail(@PathVariable("id") Long id, Model model) {
        Application application = applicationService.getApplicationById(id);
        model.addAttribute("application", application);
        return "application/detail";
    }

    // 관리자: 상태 변경 (서류적격, 심사중, 선정, 탈락)
    @PostMapping("/{id}/status")
    @PreAuthorize("hasAnyRole('MANAGER', 'ADMIN')")
    public String updateStatus(@PathVariable("id") Long id, @RequestParam("status") String status) {
        applicationService.changeStatus(id, status);
        return "redirect:/applications/" + id;
    }
}
