package kr.go.daegu.pms.cmm.web;

import kr.go.daegu.pms.cmm.service.FileService;
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
 */
@Controller
@RequestMapping("/files")
@RequiredArgsConstructor
public class FileController {

    private final FileService fileService;

    @GetMapping("/{fileId}")
    public ResponseEntity<Resource> download(@PathVariable("fileId") Long fileId) {
        FileService.Download download = fileService.loadDownload(fileId);

        // 한글 파일명은 RFC 5987(filename*=UTF-8'') 형식으로 인코딩되어 브라우저에서 깨지지 않는다
        ContentDisposition disposition = ContentDisposition.attachment()
                .filename(download.file().getOriginalName(), StandardCharsets.UTF_8)
                .build();

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, disposition.toString())
                // 항상 octet-stream 으로 내려 브라우저가 파일을 HTML/스크립트로 해석해 실행하지 않도록 함
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .contentLength(download.file().getFileSize())
                .body(download.resource());
    }
}
