package kr.go.daegu.pms.user.web;

import kr.go.daegu.pms.user.service.UserManageService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

/** 내 정보 (로그인한 본인) */
@Controller
@RequiredArgsConstructor
public class MyPageController {

    private final UserManageService userManageService;

    @GetMapping("/mypage")
    public String myPage(Model model) {
        model.addAttribute("me", userManageService.selectMyInfo());
        return "user/mypage";
    }
}
