package kr.go.daegu.pms.controller;

import kr.go.daegu.pms.domain.Announcement;
import kr.go.daegu.pms.domain.Application;
import kr.go.daegu.pms.service.AnnouncementService;
import kr.go.daegu.pms.service.ApplicationService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

@Controller
@RequiredArgsConstructor
public class MainController {

    private final AnnouncementService announcementService;
    private final ApplicationService applicationService;

    @GetMapping("/")
    public String index(Model model, Authentication authentication) {
        // 주요 대시보드 통계 지표
        int activeAnnouncements = announcementService.getActiveCount();
        int totalApplications = applicationService.getTotalApplicationsCount();
        int selectedApplications = applicationService.getSelectedApplicationsCount();

        // 최신 공고 5건
        List<Announcement> recentAnnouncements = announcementService.getAnnouncements(null, "OPEN", null);
        if (recentAnnouncements.size() > 5) {
            recentAnnouncements = recentAnnouncements.subList(0, 5);
        }

        model.addAttribute("activeAnnouncements", activeAnnouncements);
        model.addAttribute("totalApplications", totalApplications);
        model.addAttribute("selectedApplications", selectedApplications);
        model.addAttribute("announcements", recentAnnouncements);

        if (authentication != null && authentication.isAuthenticated()) {
            model.addAttribute("currentUsername", authentication.getName());
            model.addAttribute("authorities", authentication.getAuthorities());
        }

        return "index";
    }

    @GetMapping("/login")
    public String login(@RequestParam(value = "error", required = false) String error, Model model) {
        if (error != null) {
            model.addAttribute("loginError", "아이디 또는 비밀번호가 일치하지 않습니다.");
        }
        return "login";
    }
}
