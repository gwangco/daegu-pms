package kr.go.daegu.pms.service;

import kr.go.daegu.pms.domain.Application;
import kr.go.daegu.pms.mapper.ApplicationMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ApplicationService {

    private final ApplicationMapper applicationMapper;

    @Transactional(readOnly = true)
    public List<Application> getApplications(Long announcementId, String companyUserId, String status) {
        return applicationMapper.selectApplications(announcementId, companyUserId, status);
    }

    @Transactional(readOnly = true)
    public Application getApplicationById(Long applicationId) {
        return applicationMapper.selectApplicationById(applicationId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 신청서입니다. ID: " + applicationId));
    }

    @Transactional
    public void submitApplication(Application application) {
        // 비즈니스 유효성 검증
        if (application.getTotalBudget() <= 0) {
            throw new IllegalArgumentException("총 소요 예산은 0원보다 커야 합니다.");
        }
        if (application.getRequestGrant() <= 0) {
            throw new IllegalArgumentException("신청 지원금은 0원보다 커야 합니다.");
        }
        if (application.getRequestGrant() > application.getTotalBudget()) {
            throw new IllegalArgumentException("신청 지원금은 총 소요 예산을 초과할 수 없습니다.");
        }
        
        application.setStatus("SUBMITTED");
        applicationMapper.insertApplication(application);
    }

    @Transactional
    public void changeStatus(Long applicationId, String status) {
        applicationMapper.updateApplicationStatus(applicationId, status);
    }

    @Transactional(readOnly = true)
    public int getTotalApplicationsCount() {
        return applicationMapper.countTotalApplications();
    }

    @Transactional(readOnly = true)
    public int getSelectedApplicationsCount() {
        return applicationMapper.countSelectedApplications();
    }
}
