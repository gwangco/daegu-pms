package kr.go.daegu.pms.faq.web;

import java.util.List;

import org.egovframe.rte.fdl.property.EgovPropertyService;
import org.egovframe.rte.ptl.mvc.tags.ui.pagination.PaginationInfo;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.ui.ModelMap;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.support.SessionStatus;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import kr.go.daegu.pms.faq.service.FaqService;
import kr.go.daegu.pms.faq.service.FaqVO;

import jakarta.annotation.Resource;
import jakarta.validation.Valid;

/**
 * @Class Name : FaqController.java
 * @Description : Faq Controller Class
 * @Modification Information
 * @
 * @ 수정일 수정자 수정내용
 * @ --------- --------- -------------------------------
 * @ 2026-10-22 author 최초생성
 *
 * @author author
 * @since 2026-10-22
 * @version 1.0.0
 * @see
 *
 * Copyright (C) All right reserved.
 */
@Controller
public class FaqController {

	/** FaqService */
	@Resource(name = "faqService")
	private FaqService faqService;

	/** EgovPropertyService */
	@Resource(name = "propertiesService")
	private EgovPropertyService propertiesService;

	/**
	 * Faq 목록을 조회한다. (pageing)
	 * @param faqVO - 조회할 정보가 담긴 FaqDefaultVO
	 * @param model
	 * @return "faqList"
	 * @exception Exception
	 */
	@GetMapping("/faq/faqList.do")
	public String selectFaqList(@ModelAttribute("faqVO") FaqVO faqVO, ModelMap model) throws Exception {

		/** EgovPropertyService.sample */
		faqVO.setPageUnit(propertiesService.getInt("pageUnit"));
		faqVO.setPageSize(propertiesService.getInt("pageSize"));

		/** pageing setting */
		PaginationInfo paginationInfo = new PaginationInfo();
		paginationInfo.setCurrentPageNo(faqVO.getPageIndex());
		paginationInfo.setRecordCountPerPage(faqVO.getPageUnit());
		paginationInfo.setPageSize(faqVO.getPageSize());

		faqVO.setFirstIndex(paginationInfo.getFirstRecordIndex());
		faqVO.setLastIndex(paginationInfo.getLastRecordIndex());
		faqVO.setRecordCountPerPage(paginationInfo.getRecordCountPerPage());

		/** List */
		List<?> faqList = faqService.selectFaqList(faqVO);
		model.addAttribute("resultList", faqList);

		/** Count */
		int totCnt = faqService.selectFaqListTotCnt(faqVO);
		paginationInfo.setTotalRecordCount(totCnt);

		/** Pagination */
		model.addAttribute("paginationInfo", paginationInfo);

		return "faq/faqList";
	}

	/**
	 * 글 등록 화면을 조회한다.
	 * @param faqVO - 목록 조회조건 정보가 담긴 VO
	 * @param model
	 * @return "faqRegister"
	 * @exception Exception
	 */
	@PostMapping("/faq/addFaqView.do")
	public String addFaqView(@ModelAttribute("faqVO") FaqVO faqVO, Model model) throws Exception {

		model.addAttribute("faqVO", faqVO);

		return "faq/faqRegister";
	}

	/**
	 * 글을 등록한다.
	 * @param faqVO - 등록할 정보가 담긴 VO
	 * @param bindingResult - @ModelAttrubute에 대한 바인딩 검증 결과
	 * @param model
	 * @param status
	 * @return "redirect:/faq/faqList.do"
	 * @exception Exception
	 */
	@PostMapping("/faq/addFaq.do")
	public String addFaq(@Valid @ModelAttribute("faqVO") FaqVO faqVO, BindingResult bindingResult, Model model, SessionStatus status) throws Exception {

		// Server-Side Validation
		if (bindingResult.hasErrors()) {
			model.addAttribute("faqVO", faqVO);
			return "faq/faqRegister";
		}

		faqService.insertFaq(faqVO);
		status.setComplete();

		return "redirect:/faq/faqList.do";
	}

	/**
	 * 글 수정화면을 조회한다.
	 * @param faqId - 수정할 글의 faqId
	 * @param model
	 * @return "faqRegister"
	 * @exception Exception
	 */
	@PostMapping("/faq/updateFaqView.do")
	public String updateFaqView(@ModelAttribute("faqVO") FaqVO faqVO ,Model model) throws Exception {

		FaqVO detail = faqService.selectFaq(faqVO);
		detail.setSearchCondition(faqVO.getSearchCondition());
		detail.setSearchKeyword(faqVO.getSearchKeyword());
		detail.setPageIndex(faqVO.getPageIndex());

		model.addAttribute("faqVO", detail);
		
		return "faq/faqRegister";
	}

	/**
	 * 글을 수정한다.
	 * @param faqVO - 수정할 정보가 담긴 VO
	 * @param bindingResult - @ModelAttrubute에 대한 바인딩 검증 결과
	 * @param model
	 * @param status
	 * @return "redirect:/faq/faqList.do"
	 * @exception Exception
	 */
	@PostMapping("/faq/updateFaq.do")
	public String updateFaq(@Valid @ModelAttribute("faqVO") FaqVO faqVO, BindingResult bindingResult, Model model, RedirectAttributes redirectAttributes, SessionStatus status) throws Exception {

		// Server-Side Validation
		if (bindingResult.hasErrors()) {
			model.addAttribute("faqVO", faqVO);
			return "faq/faqRegister";
		}

		faqService.updateFaq(faqVO);
		status.setComplete();

		redirectAttributes.addAttribute("searchCondition", faqVO.getSearchCondition());
		redirectAttributes.addAttribute("searchKeyword", faqVO.getSearchKeyword());
		redirectAttributes.addAttribute("pageIndex", faqVO.getPageIndex());

		return "redirect:/faq/faqList.do";
	}

	/**
	 * 글을 삭제한다.
	 * @param faqVO - 삭제할 정보가 담긴 VO
	 * @param model
	 * @param status
	 * @return "redirect:/faq/faqList.do"
	 * @exception Exception
	 */
	@PostMapping("/faq/deleteFaq.do")
	public String deleteFaq(@ModelAttribute("faqVO") FaqVO faqVO, RedirectAttributes redirectAttributes, SessionStatus status) throws Exception {
		faqService.deleteFaq(faqVO);
		status.setComplete();

		redirectAttributes.addAttribute("searchCondition", faqVO.getSearchCondition());
		redirectAttributes.addAttribute("searchKeyword", faqVO.getSearchKeyword());
		redirectAttributes.addAttribute("pageIndex", faqVO.getPageIndex());

		return "redirect:/faq/faqList.do";
	}

}
