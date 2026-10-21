package kr.go.daegu.pms.notice.web;

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

import kr.go.daegu.pms.notice.service.NoticeService;
import kr.go.daegu.pms.notice.service.NoticeVO;

import jakarta.annotation.Resource;
import jakarta.validation.Valid;

/**
 * @Class Name : NoticeController.java
 * @Description : Notice Controller Class
 * @Modification Information
 * @
 * @ 수정일 수정자 수정내용
 * @ --------- --------- -------------------------------
 * @ 2026-10-21 author 최초생성
 *
 * @author author
 * @since 2026-10-21
 * @version 1.0.0
 * @see
 *
 * Copyright (C) All right reserved.
 */
@Controller
public class NoticeController {

	/** NoticeService */
	@Resource(name = "noticeService")
	private NoticeService noticeService;

	/** EgovPropertyService */
	@Resource(name = "propertiesService")
	private EgovPropertyService propertiesService;

	/**
	 * Notice 목록을 조회한다. (pageing)
	 * @param noticeVO - 조회할 정보가 담긴 NoticeDefaultVO
	 * @param model
	 * @return "noticeList"
	 * @exception Exception
	 */
	@GetMapping("/notice/noticeList.do")
	public String selectNoticeList(@ModelAttribute("noticeVO") NoticeVO noticeVO, ModelMap model) throws Exception {

		/** EgovPropertyService.sample */
		noticeVO.setPageUnit(propertiesService.getInt("pageUnit"));
		noticeVO.setPageSize(propertiesService.getInt("pageSize"));

		/** pageing setting */
		PaginationInfo paginationInfo = new PaginationInfo();
		paginationInfo.setCurrentPageNo(noticeVO.getPageIndex());
		paginationInfo.setRecordCountPerPage(noticeVO.getPageUnit());
		paginationInfo.setPageSize(noticeVO.getPageSize());

		noticeVO.setFirstIndex(paginationInfo.getFirstRecordIndex());
		noticeVO.setLastIndex(paginationInfo.getLastRecordIndex());
		noticeVO.setRecordCountPerPage(paginationInfo.getRecordCountPerPage());

		/** List */
		List<?> noticeList = noticeService.selectNoticeList(noticeVO);
		model.addAttribute("resultList", noticeList);

		/** Count */
		int totCnt = noticeService.selectNoticeListTotCnt(noticeVO);
		paginationInfo.setTotalRecordCount(totCnt);

		/** Pagination */
		model.addAttribute("paginationInfo", paginationInfo);

		return "notice/noticeList";
	}

	/**
	 * 글 등록 화면을 조회한다.
	 * @param noticeVO - 목록 조회조건 정보가 담긴 VO
	 * @param model
	 * @return "noticeRegister"
	 * @exception Exception
	 */
	@PostMapping("/notice/addNoticeView.do")
	public String addNoticeView(@ModelAttribute("noticeVO") NoticeVO noticeVO, Model model) throws Exception {

		model.addAttribute("noticeVO", noticeVO);

		return "notice/noticeRegister";
	}

	/**
	 * 글을 등록한다.
	 * @param noticeVO - 등록할 정보가 담긴 VO
	 * @param bindingResult - @ModelAttrubute에 대한 바인딩 검증 결과
	 * @param model
	 * @param status
	 * @return "redirect:/notice/noticeList.do"
	 * @exception Exception
	 */
	@PostMapping("/notice/addNotice.do")
	public String addNotice(@Valid @ModelAttribute("noticeVO") NoticeVO noticeVO, BindingResult bindingResult, Model model, SessionStatus status) throws Exception {

		// Server-Side Validation
		if (bindingResult.hasErrors()) {
			model.addAttribute("noticeVO", noticeVO);
			return "notice/noticeRegister";
		}

		noticeService.insertNotice(noticeVO);
		status.setComplete();

		return "redirect:/notice/noticeList.do";
	}

	/**
	 * 글 수정화면을 조회한다.
	 * @param noticeId - 수정할 글의 noticeId
	 * @param model
	 * @return "noticeRegister"
	 * @exception Exception
	 */
	@PostMapping("/notice/updateNoticeView.do")
	public String updateNoticeView(@ModelAttribute("noticeVO") NoticeVO noticeVO ,Model model) throws Exception {

		NoticeVO detail = noticeService.selectNotice(noticeVO);
		detail.setSearchCondition(noticeVO.getSearchCondition());
		detail.setSearchKeyword(noticeVO.getSearchKeyword());
		detail.setPageIndex(noticeVO.getPageIndex());

		model.addAttribute("noticeVO", detail);
		
		return "notice/noticeRegister";
	}

	/**
	 * 글을 수정한다.
	 * @param noticeVO - 수정할 정보가 담긴 VO
	 * @param bindingResult - @ModelAttrubute에 대한 바인딩 검증 결과
	 * @param model
	 * @param status
	 * @return "redirect:/notice/noticeList.do"
	 * @exception Exception
	 */
	@PostMapping("/notice/updateNotice.do")
	public String updateNotice(@Valid @ModelAttribute("noticeVO") NoticeVO noticeVO, BindingResult bindingResult, Model model, RedirectAttributes redirectAttributes, SessionStatus status) throws Exception {

		// Server-Side Validation
		if (bindingResult.hasErrors()) {
			model.addAttribute("noticeVO", noticeVO);
			return "notice/noticeRegister";
		}

		noticeService.updateNotice(noticeVO);
		status.setComplete();

		redirectAttributes.addAttribute("searchCondition", noticeVO.getSearchCondition());
		redirectAttributes.addAttribute("searchKeyword", noticeVO.getSearchKeyword());
		redirectAttributes.addAttribute("pageIndex", noticeVO.getPageIndex());

		return "redirect:/notice/noticeList.do";
	}

	/**
	 * 글을 삭제한다.
	 * @param noticeVO - 삭제할 정보가 담긴 VO
	 * @param model
	 * @param status
	 * @return "redirect:/notice/noticeList.do"
	 * @exception Exception
	 */
	@PostMapping("/notice/deleteNotice.do")
	public String deleteNotice(@ModelAttribute("noticeVO") NoticeVO noticeVO, RedirectAttributes redirectAttributes, SessionStatus status) throws Exception {
		noticeService.deleteNotice(noticeVO);
		status.setComplete();

		redirectAttributes.addAttribute("searchCondition", noticeVO.getSearchCondition());
		redirectAttributes.addAttribute("searchKeyword", noticeVO.getSearchKeyword());
		redirectAttributes.addAttribute("pageIndex", noticeVO.getPageIndex());

		return "redirect:/notice/noticeList.do";
	}

}
