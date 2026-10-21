package kr.go.daegu.pms.service;

import kr.go.daegu.pms.domain.AttachFile;
import org.springframework.core.io.Resource;
import org.springframework.web.multipart.MultipartFile;

/**
 * 첨부파일 저장/다운로드 서비스 (전자정부 공통컴포넌트 EgovFileMngService 단순화)
 */
public interface FileService {

    /**
     * 업로드 파일을 검증 후 디스크에 저장하고 메타데이터를 DB에 등록한다.
     * 호출 측 트랜잭션이 롤백되면 디스크에 저장된 파일도 함께 삭제된다.
     */
    AttachFile storeFile(MultipartFile multipartFile);

    /** 다운로드 권한을 검사한 뒤 파일 메타데이터와 실제 파일 리소스를 반환 */
    Download loadDownload(Long fileId);

    record Download(AttachFile file, Resource resource) {}
}
