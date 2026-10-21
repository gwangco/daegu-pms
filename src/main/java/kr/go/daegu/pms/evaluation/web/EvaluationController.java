package kr.go.daegu.pms.evaluation.web;

import kr.go.daegu.pms.cmm.message.AppMessages;

import kr.go.daegu.pms.cmm.exception.BusinessException;
import kr.go.daegu.pms.evaluation.service.EvaluationVO;
import kr.go.daegu.pms.application.service.ApplicationService;
import kr.go.daegu.pms.evaluation.service.EvaluationService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * 외부 평가위원 온라인 채점 (정량 40점 + 정성 60점)
 */
@Controller
@RequestMapping("/evaluator/evaluations")
@PreAuthorize("hasRole('EVALUATOR')")
@RequiredArgsConstructor
public class EvaluationController {

    private final AppMessages messages;

    private final EvaluationService evaluationService;
    private final ApplicationService applicationService;

    @GetMapping
    public String list(Model model) {
        model.addAttribute("evaluations", evaluationService.selectMyEvaluationList());
        return "evaluation/list";
    }

    @GetMapping("/{id}")
    public String form(@PathVariable("id") Long id, Model model) {
        EvaluationVO evaluation = evaluationService.selectMyEvaluation(id);
        model.addAttribute("evaluation", evaluation);
        model.addAttribute("app", applicationService.selectApplication(evaluation.getApplicationId()));
        model.addAttribute("maxQuantitative", EvaluationVO.MAX_QUANTITATIVE);
        model.addAttribute("maxQualitative", EvaluationVO.MAX_QUALITATIVE);
        return "evaluation/form";
    }

    @PostMapping("/{id}")
    public String submit(@PathVariable("id") Long id,
                         @RequestParam(value = "quantitativeScore", required = false) Integer quantitativeScore,
                         @RequestParam(value = "qualitativeScore", required = false) Integer qualitativeScore,
                         @RequestParam(value = "evalComment", required = false) String evalComment,
                         RedirectAttributes redirectAttributes) {
        try {
            if (quantitativeScore == null || qualitativeScore == null) {
                throw messages.business("evaluation.scores.required");
            }
            evaluationService.submitScore(id, quantitativeScore, qualitativeScore, evalComment);
            redirectAttributes.addFlashAttribute("successMessage", messages.text("evaluation.submitted"));
            return "redirect:/evaluator/evaluations";
        } catch (BusinessException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
            return "redirect:/evaluator/evaluations/" + id;
        }
    }
}
