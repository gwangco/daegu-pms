package kr.go.daegu.pms.cmm.service.impl;

import org.apache.ibatis.annotations.Param;
import org.egovframe.rte.psl.dataaccess.mapper.EgovMapper;

/**
 * 첨부파일이 연결된 업무를 찾는 Mapper (다운로드 권한 판단용).
 * 첨부파일 자체의 등록·조회는 공통컴포넌트 EgovFileMngService 가 담당한다.
 */
@EgovMapper
public interface FileMapper {

    /** 이 첨부파일을 사업계획서로 연결한 신청서 ID (없으면 null) */
    Long selectApplicationIdByPlanFile(@Param("atchFileId") String atchFileId);

    /** 이 첨부파일을 증빙으로 연결한 정산 건의 신청서 ID (없으면 null) */
    Long selectApplicationIdByReceiptFile(@Param("atchFileId") String atchFileId);
}
