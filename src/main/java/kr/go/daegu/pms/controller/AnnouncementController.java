package kr.go.daegu.pms.controller;

import kr.go.daegu.pms.domain.Announcement;
import kr.go.daegu.pms.service.AnnouncementService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Controller
@RequestMapping("/announcements")
@RequiredArgsConstructor
public class AnnouncementController {

    private final AnnouncementService announcementService;

    @GetMapping
    public String list(
            @RequestParam(value = "fieldCode", required = false) String fieldCode,
            @RequestParam(value = "status", required = false) String status,
            @RequestParam(value = "keyword", required = false) String keyword,
            Model model) {

        List<Announcement> list = announcementService.getAnnouncements(fieldCode, status, keyword);
        model.addAttribute("announcements", list);
        model.addAttribute("fieldCode", fieldCode);
        model.addAttribute("status", status);
        model.addAttribute("keyword", keyword);
        return "announcement/list";
    }

    @GetMapping("/{id}")
    public String detail(@PathVariable("id") Long id, Model model) {
        Announcement announcement = announcementService.getAnnouncementDetail(id);
        model.addAttribute("announcement", announcement);
        return "announcement/detail";
    }

    @GetMapping("/new")
    @PreAuthorize("hasAnyRole('MANAGER', 'ADMIN')")
    public String newForm(Model model) {
        model.addAttribute("announcement", new Announcement());
        return "announcement/form";
    }

    @PostMapping("/new")
    @PreAuthorize("hasAnyRole('MANAGER', 'ADMIN')")
    public String create(@ModelAttribute Announcement announcement, Authentication authentication) {
        announcement.setWriterId(authentication.getName());
        announcementService.createAnnouncement(announcement);
        return "redirect:/announcements";
    }
}
