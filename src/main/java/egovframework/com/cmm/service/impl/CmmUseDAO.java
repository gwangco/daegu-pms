package egovframework.com.cmm.service.impl;

import java.util.List;

import org.springframework.stereotype.Repository;

import egovframework.com.cmm.ComDefaultCodeVO;
import egovframework.com.cmm.service.CmmnDetailCode;

/**
 * 공통코드등 전체 업무에서 공용해서 사용해야 하는 서비스를 정의하기위한 데이터 접근 클래스
 * 
 * @author 공통 서비스 개발팀 이삼섭
 * @since 2009.03.11
 * @version 1.0
 * @see
 *
 *      <pre>
 *  == 개정이력(Modification Information) ==
 *
 *   수정일      수정자           수정내용
 *  -------    --------    ---------------------------
 *   2009.03.11  이삼섭          최초 생성
 *   2025.07.16  이백행          2025년 컨트리뷰션 불필요한 예외 제거
 *   2026.10.22  gwangco         D-PMS 적용: 조직정보·그룹정보 코드 조회 제거 (COMTNORGNZTINFO·COMTNAUTHORGROUPINFO 미사용)
 *
 *      </pre>
 */
@Repository("cmmUseDAO")
public class CmmUseDAO extends EgovComAbstractDAO {

	/**
	 * 주어진 조건에 따른 공통코드를 불러온다.
	 * 
	 * @param comDefaultCodeVO
	 * @return
	 */
	public List<CmmnDetailCode> selectCmmCodeDetail(ComDefaultCodeVO comDefaultCodeVO) {
		return selectList("CmmUseDAO.selectCmmCodeDetail", comDefaultCodeVO);
	}
}
