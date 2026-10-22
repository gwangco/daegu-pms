package kr.go.daegu.pms.user.web;

import kr.go.daegu.pms.cmm.exception.BusinessException;
import kr.go.daegu.pms.cmm.message.AppMessages;
import kr.go.daegu.pms.cmm.pagination.PagingHelper;
import kr.go.daegu.pms.user.service.LoginHistorySearchVO;
import kr.go.daegu.pms.user.service.LoginLockPolicy;
import kr.go.daegu.pms.user.service.LoginPolicyService;
import kr.go.daegu.pms.user.service.LoginResult;
import kr.go.daegu.pms.user.service.UserManageService;
import kr.go.daegu.pms.user.service.UserSearchVO;
import lombok.RequiredArgsConstructor;
import org.egovframe.rte.ptl.mvc.tags.ui.pagination.PaginationInfo;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/** 시스템 관리: 사용자 관리·접속 기록 (총괄관리자) */
@Controller
@RequestMapping("/admin")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
public class AdminUserController {

    private final UserManageService userManageService;
    private final LoginPolicyService loginPolicyService;
    private final LoginLockPolicy lockPolicy;
    private final PagingHelper pagingHelper;
    private final AppMessages messages;

    @GetMapping("/users")
    public String userList(@ModelAttribute("searchVO") UserSearchVO searchVO, Model model) {
        PaginationInfo paginationInfo = pagingHelper.prepare(searchVO);

        model.addAttribute("users", userManageService.selectUserList(searchVO));
        paginationInfo.setTotalRecordCount(userManageService.selectUserListTotCnt(searchVO));
        model.addAttribute("paginationInfo", paginationInfo);
        model.addAttribute("lockPolicy", lockPolicy);
        return "admin/user-list";
    }

    @PostMapping("/users/{userId}/unlock")
    public String unlock(@PathVariable("userId") String userId, RedirectAttributes redirectAttributes) {
        try {
            userManageService.updateUserUnlock(userId);
            redirectAttributes.addFlashAttribute("successMessage", messages.text("user.unlocked"));
        } catch (BusinessException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/admin/users";
    }

    @GetMapping("/login-history")
    public String loginHistory(@ModelAttribute("searchVO") LoginHistorySearchVO searchVO, Model model) {
        PaginationInfo paginationInfo = pagingHelper.prepare(searchVO);

        model.addAttribute("histories", loginPolicyService.selectLoginHistoryList(searchVO));
        paginationInfo.setTotalRecordCount(loginPolicyService.selectLoginHistoryListTotCnt(searchVO));
        model.addAttribute("paginationInfo", paginationInfo);
        model.addAttribute("results", LoginResult.values());
        return "admin/login-history";
    }
}
