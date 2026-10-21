package kr.go.daegu.pms.cmm.service.impl;

import kr.go.daegu.pms.cmm.service.CommonCodeVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface CommonCodeMapper {
    List<CommonCodeVO> selectCodeList(@Param("groupId") String groupId);
    int selectCodeCount(@Param("groupId") String groupId, @Param("codeId") String codeId);
}
