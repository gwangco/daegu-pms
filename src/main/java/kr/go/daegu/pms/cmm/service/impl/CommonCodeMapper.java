package kr.go.daegu.pms.cmm.service.impl;

import kr.go.daegu.pms.cmm.service.CommonCodeVO;
import org.egovframe.rte.psl.dataaccess.mapper.EgovMapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@EgovMapper("commonCodeMapper")
public interface CommonCodeMapper {
    List<CommonCodeVO> selectCodeList(@Param("groupId") String groupId);
    int selectCodeCount(@Param("groupId") String groupId, @Param("codeId") String codeId);
}
