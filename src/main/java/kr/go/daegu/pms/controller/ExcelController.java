package kr.go.daegu.pms.controller;

import kr.go.daegu.pms.domain.ApplicationSearchVO;
import kr.go.daegu.pms.service.ApplicationService;
import kr.go.daegu.pms.service.ExcelService;
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

import java.io.IOException;
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
    private final Clock clock;

    /** 목록 화면과 같은 검색 조건으로 전체 건(페이징 없음)을 엑셀로 내려받는다 */
    @GetMapping("/applications")
    @PreAuthorize("hasAnyRole('MANAGER', 'ADMIN')")
    public ResponseEntity<byte[]> downloadApplicationsExcel(@ModelAttribute ApplicationSearchVO searchVO) throws IOException {
        byte[] excelBytes = excelService.exportApplicationsToExcel(applicationService.selectApplicationList(searchVO));

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
