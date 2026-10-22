package kr.go.daegu.pms.user.service.impl;

import kr.go.daegu.pms.user.service.LoginHistorySearchVO;
import kr.go.daegu.pms.user.service.LoginHistoryVO;
import org.egovframe.rte.psl.dataaccess.mapper.EgovMapper;

import java.util.List;

@EgovMapper
public interface LoginHistoryMapper {

    int insertLoginHistory(LoginHistoryVO history);

    List<LoginHistoryVO> selectLoginHistoryList(LoginHistorySearchVO searchVO);

    int selectLoginHistoryListTotCnt(LoginHistorySearchVO searchVO);
}
