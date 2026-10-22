package kr.go.daegu.pms.cmm.web;

import kr.go.daegu.pms.announcement.service.AnnouncementSearchVO;
import kr.go.daegu.pms.announcement.service.AnnouncementService;
import kr.go.daegu.pms.announcement.service.AnnouncementVO;
import kr.go.daegu.pms.application.service.ApplicationService;
import kr.go.daegu.pms.cmm.message.AppMessages;
import kr.go.daegu.pms.cmm.service.DashboardService;
import kr.go.daegu.pms.notice.service.NoticeService;
import kr.go.daegu.pms.notice.service.NoticeVO;
import kr.go.daegu.pms.user.service.LoginLockPolicy;
import kr.go.daegu.pms.user.web.LoginFailureHandler;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
@RequiredArgsConstructor
public class MainController {

    private static final int RECENT_COUNT = 5;

    private final AnnouncementService announcementService;
    private final ApplicationService applicationService;
    private final NoticeService noticeService;
    private final DashboardService dashboardService;
    private final AppMessages messages;
    private final LoginLockPolicy lockPolicy;

    @GetMapping("/")
    public String index(Model model) {
        // 로그인 사용자의 처리할 일 (비로그인이면 빈 목록)
        model.addAttribute("myTasks", dashboardService.selectMyTaskList());

        // 주요 대시보드 통계 지표
        model.addAttribute("activeAnnouncements", announcementService.selectActiveAnnouncementCount());
        model.addAttribute("totalApplications", applicationService.selectTotalApplicationCount());
        model.addAttribute("selectedApplications", applicationService.selectSelectedApplicationCount());

        // 최신 접수중 공고 5건 (SQL LIMIT 으로 필요한 건수만 조회)
        AnnouncementSearchVO searchVO = new AnnouncementSearchVO();
        searchVO.setStatus(AnnouncementVO.STATUS_OPEN);
        searchVO.setRecordCountPerPage(RECENT_COUNT);
        model.addAttribute("announcements", announcementService.selectAnnouncementList(searchVO));

        // 최근 공지사항 5건 (중요 공지 우선)
        NoticeVO noticeSearchVO = new NoticeVO();
        noticeSearchVO.setRecordCountPerPage(RECENT_COUNT);
        model.addAttribute("notices", noticeService.selectNoticeList(noticeSearchVO));

        // 전략산업별 예산 배분 차트 데이터 (DB 집계)
        model.addAttribute("fieldBudgetStats", announcementService.selectFieldBudgetStatList());
        return "index";
    }

    @GetMapping("/login")
    public String login(@RequestParam(value = "error", required = false) String error, Model model) {
        if (error != null) {
            // 아이디가 없는 경우와 비밀번호가 틀린 경우는 같은 문구로 안내한다 (계정 존재 여부 노출 방지)
            String message = switch (error) {
                case LoginFailureHandler.ERROR_LOCKED ->
                        messages.text("login.locked", lockPolicy.getMaxFailures(), lockPolicy.getLockMinutes());
                case LoginFailureHandler.ERROR_DISABLED -> messages.text("login.disabled");
                default -> messages.text("login.failed");
            };
            model.addAttribute("loginError", message);
        }
        model.addAttribute("loginPolicy", messages.text("login.policy", lockPolicy.getMaxFailures(), lockPolicy.getLockMinutes()));
        return "login";
    }

    @GetMapping("/privacy")
    public String privacy() {
        return "privacy";
    }
}
