package kr.go.daegu.pms.cmm.service.impl;

import kr.go.daegu.pms.cmm.message.AppMessages;
import kr.go.daegu.pms.cmm.service.DashboardCountVO;
import kr.go.daegu.pms.cmm.service.DashboardService;
import kr.go.daegu.pms.cmm.service.TaskVO;
import kr.go.daegu.pms.cmm.util.SecurityUtil;
import kr.go.daegu.pms.user.service.LoginLockPolicy;
import lombok.RequiredArgsConstructor;
import org.egovframe.rte.fdl.cmmn.EgovAbstractServiceImpl;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * 역할별 "처리할 일". 로그인하자마자 오늘 처리해야 할 업무와 바로가기를 보여 준다.
 * 건수만 조회하므로 다른 사람의 데이터 내용은 노출되지 않는다 (기업·평가위원은 본인 기준 건수).
 */
@Service("dashboardService")
@RequiredArgsConstructor
public class DashboardServiceImpl extends EgovAbstractServiceImpl implements DashboardService {

    private final DashboardMapper dashboardMapper;
    private final LoginLockPolicy lockPolicy;
    private final AppMessages messages;

    @Override
    public List<TaskVO> selectMyTaskList() {
        String userId = SecurityUtil.getCurrentUserId();
        List<TaskVO> tasks = new ArrayList<>();
        if (userId == null) {
            return tasks;
        }
        if (SecurityUtil.hasRole(SecurityUtil.ROLE_COMPANY)) {
            DashboardCountVO c = dashboardMapper.selectCompanyTaskCounts(userId);
            tasks.add(new TaskVO(messages.text("dashboard.task.inProgress"), c.getInProgress(),
                    "/applications", false));
            tasks.add(new TaskVO(messages.text("dashboard.task.selected"), c.getSelected(),
                    "/applications?status=SELECTED", false));
            tasks.add(new TaskVO(messages.text("dashboard.task.rejectedSettlement"), c.getRejectedSettlement(),
                    "/applications?status=SELECTED", true));
        }
        if (SecurityUtil.hasRole(SecurityUtil.ROLE_EVALUATOR)) {
            tasks.add(new TaskVO(messages.text("dashboard.task.evaluationPending"),
                    dashboardMapper.selectEvaluatorPendingCount(userId), "/evaluator/evaluations", true));
        }
        if (SecurityUtil.isStaff()) {
            DashboardCountVO c = dashboardMapper.selectStaffTaskCounts();
            tasks.add(new TaskVO(messages.text("dashboard.task.submitted"), c.getSubmitted(),
                    "/applications?status=SUBMITTED", false));
            tasks.add(new TaskVO(messages.text("dashboard.task.readyToSelect"), c.getReadyToSelect(),
                    "/applications?status=EVALUATING", false));
            tasks.add(new TaskVO(messages.text("dashboard.task.settlementRequested"), c.getSettlementRequested(),
                    "/settlements/review", false));
        }
        if (SecurityUtil.hasRole(SecurityUtil.ROLE_ADMIN)) {
            LocalDate today = lockPolicy.now().toLocalDate();
            DashboardCountVO c = dashboardMapper.selectAdminTaskCounts(lockPolicy.activeLockSince(), today.atStartOfDay());
            tasks.add(new TaskVO(messages.text("dashboard.task.pendingMembers"), c.getPendingMembers(),
                    "/admin/users?pendingOnly=Y", false));
            tasks.add(new TaskVO(messages.text("dashboard.task.lockedUsers"), c.getLockedUsers(),
                    "/admin/users?lockedOnly=Y", true));
            tasks.add(new TaskVO(messages.text("dashboard.task.loginFailuresToday"), c.getLoginFailuresToday(),
                    "/admin/login-history?fromDate=" + today, true));
        }
        return tasks;
    }
}
