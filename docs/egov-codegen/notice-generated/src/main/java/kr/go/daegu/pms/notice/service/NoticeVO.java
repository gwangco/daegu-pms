package kr.go.daegu.pms.notice.service;

import org.egovframe.rte.ptl.reactive.validation.EgovNullCheck;

/**
 * @Class Name : NoticeVO.java
 * @Description : NoticeVO Class
 * @Modification Information
 * @
 * @  수정일      수정자              수정내용
 * @ ---------   ---------   -------------------------------
 * @ 2026-10-21   author   최초생성
 *
 * @author author
 * @since 2026-10-21
 * @version 1.0.0
 * @see
 *
 *  Copyright (C) All right reserved.
 */
public class NoticeVO extends NoticeDefaultVO {

	private static final long serialVersionUID = 1L;

	/** notice_id */
	private java.lang.String noticeId;

	/** title */
	// @EgovNullCheck(message="title is required") // egovframe-Todo: Null Check 적용시 좌측 주석 코드 참고
	private java.lang.String title;

	/** content */
	// @EgovNullCheck(message="content is required") // egovframe-Todo: Null Check 적용시 좌측 주석 코드 참고
	private java.lang.String content;

	/** important_yn */
	// @EgovNullCheck(message="importantYn is required") // egovframe-Todo: Null Check 적용시 좌측 주석 코드 참고
	private java.lang.String importantYn;

	/** view_count */
	// @EgovNullCheck(message="viewCount is required") // egovframe-Todo: Null Check 적용시 좌측 주석 코드 참고
	private java.lang.Integer viewCount;

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

	public java.lang.String getNoticeId() {
		return noticeId;
	}

	public void setNoticeId(java.lang.String noticeId) {
		this.noticeId = noticeId;
	}

	public java.lang.String getTitle() {
		return title;
	}

	public void setTitle(java.lang.String title) {
		this.title = title;
	}

	public java.lang.String getContent() {
		return content;
	}

	public void setContent(java.lang.String content) {
		this.content = content;
	}

	public java.lang.String getImportantYn() {
		return importantYn;
	}

	public void setImportantYn(java.lang.String importantYn) {
		this.importantYn = importantYn;
	}

	public java.lang.Integer getViewCount() {
		return viewCount;
	}

	public void setViewCount(java.lang.Integer viewCount) {
		this.viewCount = viewCount;
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
