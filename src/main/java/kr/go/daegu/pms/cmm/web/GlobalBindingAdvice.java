package kr.go.daegu.pms.cmm.web;

import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.InitBinder;

/**
 * 모든 컨트롤러의 @ModelAttribute 바인딩에 공통 적용되는 규칙.
 *
 * 시큐어코딩 - 대량 할당(Mass Assignment) 방지:
 * 서버가 계산하거나 서버가 결정해야 하는 필드는 HTTP 요청 파라미터로 값을 덮어쓸 수 없게 막는다.
 * 예) ?recordCountPerPage=0 으로 페이징을 우회해 전체 데이터를 한 번에 조회하는 시도,
 *     ?companyUserId=company2 로 다른 기업 신청서를 조회하려는 시도,
 *     신청서 제출 시 status=SELECTED 를 끼워 넣어 바로 선정 상태로 만드는 시도
 */
@ControllerAdvice
public class GlobalBindingAdvice {

    @InitBinder
    public void initBinder(WebDataBinder binder) {
        binder.setDisallowedFields(
                "firstIndex", "recordCountPerPage",                      // 페이징 (서버 계산)
                "companyUserId", "evaluatorUserId", "writerId",           // 소유자 (로그인 정보로 결정)
                "createdBy", "reviewedBy", "viewCount",                   // 감사/집계 컬럼
                "planFileId", "receiptFileId",                            // 파일 ID (업로드 결과로 결정)
                "totalAmount", "selfFunding", "totalScore"                // 서버 재계산 금액/점수
        );
    }
}
