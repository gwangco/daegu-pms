package kr.go.daegu.pms.controller;

import kr.go.daegu.pms.domain.Application;
import kr.go.daegu.pms.service.ApplicationService;
import kr.go.daegu.pms.service.ExcelService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Controller
@RequestMapping("/excel")
@RequiredArgsConstructor
public class ExcelController {

    private final ApplicationService applicationService;
    private final ExcelService excelService;

    @GetMapping("/applications")
    @PreAuthorize("hasAnyRole('MANAGER', 'ADMIN')")
    public ResponseEntity<byte[]> downloadApplicationsExcel(
            @RequestParam(value = "announcementId", required = false) Long announcementId,
            @RequestParam(value = "status", required = false) String status) throws IOException {

        List<Application> applications = applicationService.getApplications(announcementId, null, status);
        byte[] excelBytes = excelService.exportApplicationsToExcel(applications);

        String dateStr = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        String fileName = URLEncoder.encode("대구_지원사업_신청현황_" + dateStr + ".xlsx", StandardCharsets.UTF_8).replaceAll("\\+", "%20");

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + fileName + "\"; filename*=UTF-8''" + fileName)
                .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(excelBytes);
    }
}
