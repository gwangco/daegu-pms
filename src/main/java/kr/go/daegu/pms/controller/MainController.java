package kr.go.daegu.pms.controller;

import kr.go.daegu.pms.domain.AnnouncementSearchVO;
import kr.go.daegu.pms.service.AnnouncementService;
import kr.go.daegu.pms.service.ApplicationService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
@RequiredArgsConstructor
public class MainController {

    private static final int RECENT_ANNOUNCEMENT_COUNT = 5;

    private final AnnouncementService announcementService;
    private final ApplicationService applicationService;

    @GetMapping("/")
    public String index(Model model) {
        // 주요 대시보드 통계 지표
        model.addAttribute("activeAnnouncements", announcementService.selectActiveAnnouncementCount());
        model.addAttribute("totalApplications", applicationService.selectTotalApplicationCount());
        model.addAttribute("selectedApplications", applicationService.selectSelectedApplicationCount());

        // 최신 접수중 공고 5건 (SQL LIMIT 으로 필요한 건수만 조회)
        AnnouncementSearchVO searchVO = new AnnouncementSearchVO();
        searchVO.setStatus("OPEN");
        searchVO.setRecordCountPerPage(RECENT_ANNOUNCEMENT_COUNT);
        model.addAttribute("announcements", announcementService.selectAnnouncementList(searchVO));

        // 전략산업별 예산 배분 차트 데이터 (DB 집계)
        model.addAttribute("fieldBudgetStats", announcementService.selectFieldBudgetStatList());
        return "index";
    }

    @GetMapping("/login")
    public String login(@RequestParam(value = "error", required = false) String error, Model model) {
        if (error != null) {
            // 아이디 존재 여부를 알려주지 않도록 하나의 메시지로 통일 (계정 열거 공격 방지)
            model.addAttribute("loginError", "아이디 또는 비밀번호가 일치하지 않습니다.");
        }
        return "login";
    }
}
