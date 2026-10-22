package kr.go.daegu.pms.cmm.service;

import egovframework.com.cmm.service.FileVO;
import org.springframework.core.io.Resource;
import org.springframework.web.multipart.MultipartFile;

/**
 * 첨부파일 저장/다운로드 서비스.
 *
 * 파일 정보는 전자정부 공통컴포넌트 첨부파일(EgovFileMngService, COMTNFILE/COMTNFILEDETAIL)에 등록하고,
 * 이 서비스는 그 앞뒤의 업무 규칙(업로드 검증, 저장 위치, 다운로드 권한)을 맡는다.
 */
public interface FileService {

    /**
     * 업로드 파일을 검증 후 디스크에 저장하고 공통컴포넌트 첨부파일로 등록한다.
     * 호출 측 트랜잭션이 롤백되면 디스크에 저장된 파일도 함께 삭제된다.
     *
     * @return 등록한 파일 정보 (atchFileId 로 업무 테이블과 연결)
     */
    FileVO storeFile(MultipartFile multipartFile);

    /** 다운로드 권한을 검사한 뒤 파일 정보와 실제 파일 리소스를 반환 */
    Download loadDownload(String atchFileId, int fileSn);

    record Download(FileVO file, Resource resource) {}
}
