package kr.go.daegu.pms.cmm.service;

import java.util.List;

/** 메인 화면 대시보드 */
public interface DashboardService {

    /** 로그인 사용자의 역할에 맞는 "처리할 일" 목록 (비로그인이면 빈 목록) */
    List<TaskVO> selectMyTaskList();
}
