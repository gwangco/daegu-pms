package kr.go.daegu.pms.cmm.service.impl;

import kr.go.daegu.pms.cmm.service.FileVO;
import org.apache.ibatis.annotations.Param;
import org.egovframe.rte.psl.dataaccess.mapper.EgovMapper;

import java.util.Optional;

@EgovMapper
public interface FileMapper {
    int insertFile(FileVO file);
    Optional<FileVO> selectFile(@Param("fileId") Long fileId);

    /** 이 파일을 사업계획서로 첨부한 신청서 ID (없으면 null) */
    Long selectApplicationIdByPlanFile(@Param("fileId") Long fileId);

    /** 이 파일을 증빙으로 첨부한 정산 건의 신청서 ID (없으면 null) */
    Long selectApplicationIdByReceiptFile(@Param("fileId") Long fileId);
}
