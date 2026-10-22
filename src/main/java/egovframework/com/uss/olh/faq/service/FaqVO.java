package egovframework.com.uss.olh.faq.service;

import org.apache.commons.lang3.builder.ToStringBuilder;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

/**
 *
 * FAQ를 처리하는 VO 클래스
 * @author 공통서비스 개발팀 박정규
 * @since 2009.04.01
 * @version 1.0
 * @see
 *
 * <pre>
 * << 개정이력(Modification Information) >>
 *
 *   수정일      수정자           수정내용
 *  -------    --------    ---------------------------
 *   2009.04.01  박정규          최초 생성
 *   2025.05.20  기여자          Lombok @Getter/@Setter 적용으로 보일러플레이트 제거
 *   2026.10.22  gwangco         D-PMS 적용: @EgovNullCheck(ptl-reactive) → Bean Validation(@NotBlank 등)과 안내 문구
 *                               분류(faqClCode)·표시 순서(sortOrdr)·사용 여부(useAt) 추가 (COMTNFAQINFO 추가 컬럼)
 *
 * </pre>
 */
@Getter
@Setter
public class FaqVO extends FaqDefaultVO {

	private static final long serialVersionUID = 1L;

	/**
	 * FAQ ID
	 */
	private String faqId;

	/**
	 * 질문 제목
	 */
	@NotBlank(message = "질문을 입력하세요.")
	@Size(max = 255, message = "질문은 255자 이내로 입력하세요.")
	private String qestnSj;

	/**
	 * 질문 내용
	 */
	@Size(max = 2500, message = "질문 내용은 2500자 이내로 입력하세요.")
	private String qestnCn;

	/**
	 * 답변 내용
	 */
	@NotBlank(message = "답변을 입력하세요.")
	@Size(max = 2500, message = "답변은 2500자 이내로 입력하세요.")
	private String answerCn;

	/**
	 * 조회 횟수
	 */
	private String inqireCo;

	/**
	 * 첨부파일 ID
	 */
	private String atchFileId;

	/**
	 * 최초등록시점
	 */
	private String frstRegisterPnttm;

	/**
	 * 최초등록자ID
	 */
	private String frstRegisterId;

	/**
	 * 최종수정시점
	 */
	private String lastUpdusrPnttm;

	/**
	 * 최종수정자ID
	 */
	private String lastUpdusrId;

	/**
	 * [D-PMS 추가] FAQ 분류 (공통코드 DPM005)
	 */
	@NotBlank(message = "분류를 선택하세요.")
	private String faqClCode;

	/**
	 * [D-PMS 추가] 분류명 (조회용)
	 */
	private String faqClCodeNm;

	/**
	 * [D-PMS 추가] 같은 분류 안의 표시 순서 (작을수록 위)
	 */
	@NotNull(message = "표시 순서를 입력하세요.")
	@Min(value = 1, message = "표시 순서는 1~999 사이로 입력하세요.")
	@Max(value = 999, message = "표시 순서는 1~999 사이로 입력하세요.")
	private Integer sortOrdr;

	/**
	 * [D-PMS 추가] 사용 여부 (N: 삭제)
	 */
	private String useAt;

	/**
	 * toString 메소드를 대치한다.
	 */
	public String toString(){
		return ToStringBuilder.reflectionToString(this);
	}
}
