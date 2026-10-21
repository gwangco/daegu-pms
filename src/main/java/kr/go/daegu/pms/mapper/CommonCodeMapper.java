package kr.go.daegu.pms.mapper;

import kr.go.daegu.pms.domain.CommonCode;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface CommonCodeMapper {
    List<CommonCode> selectCodeList(@Param("groupId") String groupId);
    int selectCodeCount(@Param("groupId") String groupId, @Param("codeId") String codeId);
}
