package kr.go.daegu.pms.faq.service;

import org.egovframe.rte.ptl.reactive.validation.EgovNullCheck;

/**
 * @Class Name : FaqVO.java
 * @Description : FaqVO Class
 * @Modification Information
 * @
 * @  수정일      수정자              수정내용
 * @ ---------   ---------   -------------------------------
 * @ 2026-10-22   author   최초생성
 *
 * @author author
 * @since 2026-10-22
 * @version 1.0.0
 * @see
 *
 *  Copyright (C) All right reserved.
 */
public class FaqVO extends FaqDefaultVO {

	private static final long serialVersionUID = 1L;

	/** faq_id */
	private java.lang.String faqId;

	/** category_code */
	// @EgovNullCheck(message="categoryCode is required") // egovframe-Todo: Null Check 적용시 좌측 주석 코드 참고
	private java.lang.String categoryCode;

	/** question */
	// @EgovNullCheck(message="question is required") // egovframe-Todo: Null Check 적용시 좌측 주석 코드 참고
	private java.lang.String question;

	/** answer */
	// @EgovNullCheck(message="answer is required") // egovframe-Todo: Null Check 적용시 좌측 주석 코드 참고
	private java.lang.String answer;

	/** sort_order */
	// @EgovNullCheck(message="sortOrder is required") // egovframe-Todo: Null Check 적용시 좌측 주석 코드 참고
	private java.lang.Integer sortOrder;

	/** use_yn */
	// @EgovNullCheck(message="useYn is required") // egovframe-Todo: Null Check 적용시 좌측 주석 코드 참고
	private java.lang.String useYn;

	/** writer_id */
	// @EgovNullCheck(message="writerId is required") // egovframe-Todo: Null Check 적용시 좌측 주석 코드 참고
	private java.lang.String writerId;

	/** created_at */
	// @EgovNullCheck(message="createdAt is required") // egovframe-Todo: Null Check 적용시 좌측 주석 코드 참고
	private java.util.Date createdAt;

	/** updated_by */
	// @EgovNullCheck(message="updatedBy is required") // egovframe-Todo: Null Check 적용시 좌측 주석 코드 참고
	private java.lang.String updatedBy;

	/** updated_at */
	// @EgovNullCheck(message="updatedAt is required") // egovframe-Todo: Null Check 적용시 좌측 주석 코드 참고
	private java.util.Date updatedAt;

	public java.lang.String getFaqId() {
		return faqId;
	}

	public void setFaqId(java.lang.String faqId) {
		this.faqId = faqId;
	}

	public java.lang.String getCategoryCode() {
		return categoryCode;
	}

	public void setCategoryCode(java.lang.String categoryCode) {
		this.categoryCode = categoryCode;
	}

	public java.lang.String getQuestion() {
		return question;
	}

	public void setQuestion(java.lang.String question) {
		this.question = question;
	}

	public java.lang.String getAnswer() {
		return answer;
	}

	public void setAnswer(java.lang.String answer) {
		this.answer = answer;
	}

	public java.lang.Integer getSortOrder() {
		return sortOrder;
	}

	public void setSortOrder(java.lang.Integer sortOrder) {
		this.sortOrder = sortOrder;
	}

	public java.lang.String getUseYn() {
		return useYn;
	}

	public void setUseYn(java.lang.String useYn) {
		this.useYn = useYn;
	}

	public java.lang.String getWriterId() {
		return writerId;
	}

	public void setWriterId(java.lang.String writerId) {
		this.writerId = writerId;
	}

	public java.util.Date getCreatedAt() {
		return createdAt;
	}

	public void setCreatedAt(java.util.Date createdAt) {
		this.createdAt = createdAt;
	}

	public java.lang.String getUpdatedBy() {
		return updatedBy;
	}

	public void setUpdatedBy(java.lang.String updatedBy) {
		this.updatedBy = updatedBy;
	}

	public java.util.Date getUpdatedAt() {
		return updatedAt;
	}

	public void setUpdatedAt(java.util.Date updatedAt) {
		this.updatedAt = updatedAt;
	}

}
