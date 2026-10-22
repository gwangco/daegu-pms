package kr.go.daegu.pms.cmm.service.impl;

import kr.go.daegu.pms.cmm.service.CommonCodeVO;
import org.apache.ibatis.annotations.Param;
import org.egovframe.rte.psl.dataaccess.mapper.EgovMapper;

import java.util.List;

@EgovMapper
public interface CommonCodeMapper {
    List<CommonCodeVO> selectCodeList(@Param("groupId") String groupId);
    int selectCodeCount(@Param("groupId") String groupId, @Param("codeId") String codeId);
}
