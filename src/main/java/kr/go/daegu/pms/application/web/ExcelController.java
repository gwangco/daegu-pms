package kr.go.daegu.pms.application.web;

import jakarta.servlet.http.HttpServletRequest;
import kr.go.daegu.pms.application.service.ApplicationVO;
import kr.go.daegu.pms.cmm.service.PrivacyAccessLogService;
import kr.go.daegu.pms.cmm.service.PrivacyAccessLogVO;
import java.util.List;
import kr.go.daegu.pms.application.service.ApplicationSearchVO;
import kr.go.daegu.pms.application.service.ApplicationService;
import kr.go.daegu.pms.application.service.ExcelService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;

import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

@Controller
@RequestMapping("/excel")
@RequiredArgsConstructor
public class ExcelController {

    private final ApplicationService applicationService;
    private final ExcelService excelService;
    private final PrivacyAccessLogService privacyAccessLogService;
    private final Clock clock;

    /** 목록 화면과 같은 검색 조건으로 전체 건(페이징 없음)을 엑셀로 내려받는다 */
    @GetMapping("/applications")
    @PreAuthorize("hasAnyRole('MANAGER', 'ADMIN')")
    public ResponseEntity<byte[]> downloadApplicationsExcel(@ModelAttribute ApplicationSearchVO searchVO,
                                                            HttpServletRequest request) {
        List<ApplicationVO> applications = applicationService.selectApplicationList(searchVO);
        byte[] excelBytes = excelService.exportApplicationsToExcel(applications);
        // 기업명·사업자등록번호가 담긴 파일을 내려받은 기록 (여러 기업이라 정보주체는 비우고 건수를 남긴다)
        privacyAccessLogService.insertPrivacyAccess(PrivacyAccessLogVO.ACTION_DOWNLOAD, PrivacyAccessLogVO.TARGET_EXCEL,
                null, null, applications.size(), request.getRemoteAddr());

        String dateStr = LocalDate.now(clock).format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        ContentDisposition disposition = ContentDisposition.attachment()
                .filename("대구_지원사업_신청현황_" + dateStr + ".xlsx", StandardCharsets.UTF_8)
                .build();

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, disposition.toString())
                .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(excelBytes);
    }
}
