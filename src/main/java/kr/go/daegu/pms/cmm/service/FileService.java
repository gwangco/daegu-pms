package kr.go.daegu.pms.cmm.service;

import egovframework.com.cmm.service.FileVO;
import org.springframework.core.io.Resource;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * 첨부파일 저장/다운로드 서비스.
 *
 * 파일 정보는 전자정부 공통컴포넌트 첨부파일(EgovFileMngService, COMTNFILE/COMTNFILEDETAIL)에 등록하고,
 * 이 서비스는 그 앞뒤의 업무 규칙(업로드 검증, 저장 위치, 다운로드 권한)을 맡는다.
 *
 * 첨부파일 묶음: COMTNFILE 한 건(ATCH_FILE_ID)에 파일 여러 개(COMTNFILEDETAIL, FILE_SN 0, 1, 2…)가 들어간다.
 * 신청서·정산은 묶음 하나에 파일 하나, 공지사항은 묶음 하나에 여러 파일을 넣는다.
 */
public interface FileService {

    /**
     * 업로드 파일을 검증 후 디스크에 저장하고 공통컴포넌트 첨부파일로 등록한다.
     * 호출 측 트랜잭션이 롤백되면 디스크에 저장된 파일도 함께 삭제된다.
     *
     * @return 등록한 파일 정보 (atchFileId 로 업무 테이블과 연결)
     */
    FileVO storeFile(MultipartFile multipartFile);

    /**
     * 여러 파일을 모두 검사한 뒤 저장해 첨부파일 묶음에 넣는다. 하나라도 규칙에 어긋나면 아무것도 저장하지 않는다.
     *
     * @param atchFileId  null 이면 새 묶음을 만들고, 있으면 그 묶음의 다음 순번부터 추가한다
     * @param maxFileSize 파일 하나의 최대 크기 (바이트)
     * @return 파일이 들어간 첨부파일 ID
     */
    String storeFiles(List<MultipartFile> files, String atchFileId, long maxFileSize);

    /** 첨부파일 묶음의 파일 목록 (순번 순). 묶음이 없으면 빈 목록 */
    List<AttachedFile> selectFiles(String atchFileId);

    /** 묶음에서 파일 하나를 지운다. 실제 파일은 트랜잭션이 커밋된 뒤에 지워진다 */
    void deleteFile(String atchFileId, int fileSn);

    /** 다운로드 권한을 검사한 뒤 파일 정보와 실제 파일 리소스를 반환 (신청서·정산 첨부, 로그인 필요) */
    Download loadDownload(String atchFileId, int fileSn);

    /** 공지 첨부 다운로드. 삭제되지 않은 공지에 연결된 파일만 내려준다 (공지처럼 비로그인도 허용) */
    Download loadNoticeFile(long nttId, int fileSn);

    record Download(FileVO file, Resource resource) {}
}
