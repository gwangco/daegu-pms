package kr.go.daegu.pms.cmm.service;

import lombok.Data;

/** 역할별 처리 대기 건수 (조회한 역할의 항목만 채워진다) */
@Data
public class DashboardCountVO {
    // 기업
    private int inProgress;
    private int selected;
    private int rejectedSettlement;
    private int drafts;
    // 담당자·관리자
    private int submitted;
    private int readyToSelect;
    private int settlementRequested;
    // 총괄관리자
    private int lockedUsers;
    private int loginFailuresToday;
    private int pendingMembers;
}
