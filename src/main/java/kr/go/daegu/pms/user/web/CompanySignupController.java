package kr.go.daegu.pms.user.web;

import kr.go.daegu.pms.cmm.exception.BusinessException;
import kr.go.daegu.pms.cmm.exception.FieldBusinessException;
import kr.go.daegu.pms.cmm.message.AppMessages;
import kr.go.daegu.pms.cmm.util.SecurityUtil;
import kr.go.daegu.pms.user.service.CompanySignupService;
import kr.go.daegu.pms.user.service.CompanySignupVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * 기업회원 가입 (비로그인).
 * 가입하면 승인 대기 상태가 되고, 총괄관리자가 승인한 뒤에 로그인할 수 있다.
 */
@Controller
@RequiredArgsConstructor
public class CompanySignupController {

    private static final String VIEW = "user/signup";

    private final CompanySignupService companySignupService;
    private final AppMessages messages;

    @GetMapping("/signup")
    public String signupForm(Model model) {
        if (SecurityUtil.getCurrentUserId() != null) {
            return "redirect:/";
        }
        model.addAttribute("signup", new CompanySignupVO());
        return VIEW;
    }

    @PostMapping("/signup")
    public String signup(@ModelAttribute("signup") CompanySignupVO signup, BindingResult bindingResult,
                         RedirectAttributes redirectAttributes) {
        if (SecurityUtil.getCurrentUserId() != null) {
            return "redirect:/";
        }
        if (bindingResult.hasErrors()) {
            return VIEW;
        }
        try {
            String userId = companySignupService.insertCompanyMember(signup);
            redirectAttributes.addFlashAttribute("signupDone", messages.text("signup.done", userId));
            return "redirect:/login";
        } catch (FieldBusinessException e) {
            // 서비스가 모은 칸별 위반을 각 입력칸 아래에 보여 준다. 비밀번호 칸은 다시 채우지 않는다(화면이 값을 출력하지 않음)
            e.getFieldErrors().forEach((field, message) -> bindingResult.rejectValue(field, "business", message));
        } catch (BusinessException e) {
            bindingResult.reject("business", e.getMessage());
        }
        return VIEW;
    }
}
