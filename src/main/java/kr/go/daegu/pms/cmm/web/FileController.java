package kr.go.daegu.pms.cmm.web;

import jakarta.servlet.http.HttpServletRequest;
import kr.go.daegu.pms.cmm.service.FileService;
import kr.go.daegu.pms.cmm.service.PrivacyAccessLogService;
import kr.go.daegu.pms.cmm.service.PrivacyAccessLogVO;
import kr.go.daegu.pms.cmm.util.SecurityUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

import java.nio.charset.StandardCharsets;

/**
 * 첨부파일 다운로드. 권한 검사는 FileService 에서 파일이 속한 과제 기준으로 수행한다.
 *
 * 주소: /files/{첨부파일 ID}/{파일 순번} (공통컴포넌트 첨부파일 키 ATCH_FILE_ID + FILE_SN). 로그인 필요.
 * 경로 패턴을 정확히 지정해 형식이 다른 값(경로 조작 문자 등)은 컨트롤러까지 오지 않고 404 가 된다.
 * 공지 첨부는 비로그인에게도 공개하므로 따로 /notices/{게시물 ID}/files/{순번} 으로 내려준다 (NoticeController).
 */
@Controller
@RequestMapping("/files")
@RequiredArgsConstructor
public class FileController {

    private final FileService fileService;
    private final PrivacyAccessLogService privacyAccessLogService;

    @GetMapping("/{atchFileId:FILE_\\d{15}}/{fileSn:\\d{1,5}}")
    public ResponseEntity<Resource> download(@PathVariable("atchFileId") String atchFileId,
                                             @PathVariable("fileSn") int fileSn,
                                             HttpServletRequest request) {
        FileService.Download download = fileService.loadDownload(atchFileId, fileSn);
        if (SecurityUtil.isStaff() || SecurityUtil.hasRole(SecurityUtil.ROLE_EVALUATOR)) {
            // 기업이 낸 서류(사업계획서·증빙)를 담당자·평가위원이 내려받은 기록. 기업 본인은 남기지 않는다
            privacyAccessLogService.insertPrivacyAccess(PrivacyAccessLogVO.ACTION_DOWNLOAD, PrivacyAccessLogVO.TARGET_FILE,
                    atchFileId + "/" + fileSn, null, 1, request.getRemoteAddr());
        }
        return attachment(download);
    }

    /** 내려받기 응답 (업무 첨부·공지 첨부 공통) */
    public static ResponseEntity<Resource> attachment(FileService.Download download) {
        // 한글 파일명은 RFC 5987(filename*=UTF-8'') 형식으로 인코딩되어 브라우저에서 깨지지 않는다
        ContentDisposition disposition = ContentDisposition.attachment()
                .filename(download.file().getOrignlFileNm(), StandardCharsets.UTF_8)
                .build();

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, disposition.toString())
                // 항상 octet-stream 으로 내려 브라우저가 파일을 HTML/스크립트로 해석해 실행하지 않도록 함
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(download.resource());
    }
}
