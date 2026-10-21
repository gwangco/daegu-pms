package kr.go.daegu.pms.cmm.service;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 첨부파일 메타데이터 (com_file 테이블).
 * 물리 파일은 {upload-dir}/{filePath}/{storedName} 에 저장된다.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FileVO {
    private Long fileId;
    private String originalName;
    private String storedName;
    private String filePath;
    private String fileExt;
    private long fileSize;
    private String createdBy;
    private LocalDateTime createdAt;
}
