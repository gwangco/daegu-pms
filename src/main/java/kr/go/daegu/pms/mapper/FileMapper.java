package kr.go.daegu.pms.mapper;

import kr.go.daegu.pms.domain.AttachFile;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.Optional;

@Mapper
public interface FileMapper {
    int insertFile(AttachFile file);
    Optional<AttachFile> selectFile(@Param("fileId") Long fileId);

    /** 이 파일을 사업계획서로 첨부한 신청서 ID (없으면 null) */
    Long selectApplicationIdByPlanFile(@Param("fileId") Long fileId);

    /** 이 파일을 증빙으로 첨부한 정산 건의 신청서 ID (없으면 null) */
    Long selectApplicationIdByReceiptFile(@Param("fileId") Long fileId);
}
