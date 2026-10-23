package kr.go.daegu.pms.cmm.service.impl;

import egovframework.com.cmm.service.EgovFileMngService;
import egovframework.com.cmm.service.FileVO;
import jakarta.annotation.Resource;
import kr.go.daegu.pms.application.service.ApplicationAccessChecker;
import kr.go.daegu.pms.cmm.message.AppMessages;
import kr.go.daegu.pms.cmm.service.AttachedFile;
import kr.go.daegu.pms.cmm.service.FileService;
import kr.go.daegu.pms.cmm.util.SecurityUtil;
import kr.go.daegu.pms.config.AppProperties;
import kr.go.daegu.pms.notice.service.NoticeService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.egovframe.rte.fdl.cmmn.EgovAbstractServiceImpl;
import org.egovframe.rte.fdl.cmmn.exception.BaseRuntimeException;
import org.egovframe.rte.fdl.cmmn.exception.FdlException;
import org.egovframe.rte.fdl.idgnr.EgovIdGnrService;
import org.springframework.core.io.FileSystemResource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Clock;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HexFormat;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

/**
 * 첨부파일 처리 구현체.
 *
 * 파일 정보 등록·조회는 전자정부 공통컴포넌트 EgovFileMngService 를 그대로 쓴다.
 * 공통컴포넌트의 업로드 도구(EgovFileMngUtil)는 쓰지 않고, 아래 검증을 거친 뒤 파일 정보만 넘긴다.
 *
 * 시큐어코딩 - 위험한 형식 파일 업로드 / 경로 조작 방지:
 *  1) 확장자 화이트리스트 검사 (application.yml app.file.allowed-extensions)
 *  2) 파일 시그니처(매직넘버) 검사 → "webshell.jsp" 를 "plan.pdf" 로 이름만 바꾼 파일 차단
 *  3) 서버 저장명은 UUID(확장자 없음) → 업로드 파일이 서버에서 실행될 여지 제거
 *  4) 저장/다운로드 경로가 업로드 루트 밖으로 벗어나지 않는지 normalize 후 검사 (../ 경로 조작 차단)
 *     DB(FILE_STRE_COURS)에는 절대 경로가 아니라 루트 아래 하위 경로(yyyy/MM)만 저장한다
 *  5) 다운로드 시 파일이 연결된 업무의 열람 권한을 재검사 (첨부파일 ID 는 순번이라 추측할 수 있으므로 IDOR 차단 필수)
 */
@Slf4j
@Service("fileService")
@RequiredArgsConstructor
public class FileServiceImpl extends EgovAbstractServiceImpl implements FileService {

    private static final int MAX_ORIGINAL_NAME_LENGTH = 200;
    private static final DateTimeFormatter SUB_DIR_FORMAT = DateTimeFormatter.ofPattern("yyyy/MM");

    /** 확장자별 파일 시그니처(파일 첫 바이트). 하나라도 일치하면 정상 파일로 판단 */
    private static final Map<String, List<String>> FILE_SIGNATURES = Map.ofEntries(
            Map.entry("pdf", List.of("25504446")),               // %PDF
            Map.entry("png", List.of("89504e470d0a1a0a")),
            Map.entry("jpg", List.of("ffd8ff")),
            Map.entry("jpeg", List.of("ffd8ff")),
            Map.entry("zip", List.of("504b0304")),               // ZIP 기반
            Map.entry("docx", List.of("504b0304")),
            Map.entry("xlsx", List.of("504b0304")),
            Map.entry("hwpx", List.of("504b0304")),
            Map.entry("hwp", List.of("d0cf11e0a1b11ae1")),       // MS 복합문서(OLE) 기반
            Map.entry("doc", List.of("d0cf11e0a1b11ae1")),
            Map.entry("xls", List.of("d0cf11e0a1b11ae1"))
    );

    /** 공통컴포넌트 첨부파일 서비스 (COMTNFILE, COMTNFILEDETAIL) */
    private final EgovFileMngService fileMngService;
    /** 첨부파일이 어느 업무(신청서·정산·공지)에 연결되어 있는지 찾는 Mapper */
    private final FileMapper fileMapper;
    private final ApplicationAccessChecker accessChecker;
    private final AppProperties appProperties;
    private final Clock clock;
    private final AppMessages messages;

    /** 첨부파일 ID 채번: FILE_000000000000001 (공통컴포넌트와 같은 Bean 이름) */
    @Resource(name = "egovFileIdGnrService")
    private EgovIdGnrService fileIdGnrService;

    /** 검사를 마친 업로드 파일 (원래 이름·확장자) */
    private record CheckedUpload(MultipartFile file, String originalName, String extension) {}

    @Override
    public FileVO storeFile(MultipartFile multipartFile) {
        if (multipartFile == null || multipartFile.isEmpty()) {
            throw messages.business("file.required");
        }
        requireLogin();
        CheckedUpload upload = check(multipartFile);

        // 첨부파일 묶음(COMTNFILE) 1건 + 파일(COMTNFILEDETAIL) 1건. 신청서·정산 첨부는 파일 1개라 순번은 0
        FileVO file = writeToDisk(upload);
        file.setAtchFileId(nextAtchFileId());
        file.setFileSn("0");
        fileMngService.insertFileInf(file);
        log.info("[파일 업로드] atchFileId={}, user={}, size={}", file.getAtchFileId(), SecurityUtil.getCurrentUserId(), file.getFileMg());
        return file;
    }

    @Override
    public String storeFiles(List<MultipartFile> files, String atchFileId, long maxFileSize) {
        if (files == null || files.isEmpty()) {
            throw messages.business("file.required");
        }
        requireLogin();

        // 먼저 전부 검사한다. 세 번째 파일이 규칙에 어긋나면 앞의 두 파일도 저장하지 않는다
        List<CheckedUpload> uploads = new ArrayList<>();
        for (MultipartFile file : files) {
            CheckedUpload upload = check(file);
            if (file.getSize() > maxFileSize) {
                throw messages.business("file.size.limit", maxFileSize / 1024 / 1024, upload.originalName());
            }
            uploads.add(upload);
        }

        // 새 묶음은 순번 0 부터, 기존 묶음은 공통컴포넌트 getMaxFileSN(현재 최대 순번 + 1)부터 이어서 매긴다
        boolean newGroup = !StringUtils.hasText(atchFileId);
        String groupId = newGroup ? nextAtchFileId() : atchFileId;
        int nextSn = newGroup ? 0 : fileMngService.getMaxFileSN(key(groupId, 0));

        List<FileVO> stored = new ArrayList<>();
        for (CheckedUpload upload : uploads) {
            FileVO file = writeToDisk(upload);
            file.setAtchFileId(groupId);
            file.setFileSn(Integer.toString(nextSn++));
            stored.add(file);
        }
        if (newGroup) {
            fileMngService.insertFileInfs(stored);    // 묶음(COMTNFILE) + 파일들(COMTNFILEDETAIL)
        } else {
            fileMngService.updateFileInfs(stored);    // 기존 묶음에 파일들만 추가
        }
        log.info("[파일 업로드] atchFileId={}, files={}, user={}", groupId, stored.size(), SecurityUtil.getCurrentUserId());
        return groupId;
    }

    @Override
    public List<AttachedFile> selectFiles(String atchFileId) {
        if (!StringUtils.hasText(atchFileId)) {
            return List.of();
        }
        FileVO group = new FileVO();
        group.setAtchFileId(atchFileId);
        return fileMngService.selectFileInfs(group).stream()
                .map(f -> new AttachedFile(Integer.parseInt(f.getFileSn()), f.getOrignlFileNm(), f.getFileExtsn(), sizeOf(f)))
                .toList();
    }

    @Override
    public void deleteFile(String atchFileId, int fileSn) {
        if (!StringUtils.hasText(atchFileId) || fileMngService.deleteFileInf(key(atchFileId, fileSn)) == 0) {
            throw messages.notFound("file.notFound");
        }
        log.info("[파일 삭제] atchFileId={}, fileSn={}, user={}", atchFileId, fileSn, SecurityUtil.getCurrentUserId());
    }

    @Override
    public Download loadDownload(String atchFileId, int fileSn) {
        FileVO file = fileMngService.selectFileInf(key(atchFileId, fileSn));
        if (file == null) {
            throw messages.notFound("file.notFound");
        }

        // 파일이 어느 과제에 속하는지 찾아 그 과제의 접근 권한으로 다운로드 권한을 판단
        Long planOwnerApplicationId = fileMapper.selectApplicationIdByPlanFile(atchFileId);
        Long receiptOwnerApplicationId = fileMapper.selectApplicationIdByReceiptFile(atchFileId);
        if (planOwnerApplicationId != null) {
            accessChecker.getReadableApplication(planOwnerApplicationId);
        } else if (receiptOwnerApplicationId != null) {
            accessChecker.getSettlementAccessibleApplication(receiptOwnerApplicationId);
        } else {
            // 어떤 업무 데이터에도 연결되지 않은 파일은 노출하지 않는다 (공지 첨부는 loadNoticeFile 로만 내려준다)
            throw messages.notFound("file.notFound");
        }
        return open(file);
    }

    @Override
    public Download loadNoticeFile(long nttId, int fileSn) {
        // 첨부 ID 는 요청 값이 아니라 공지 게시물에서 읽는다. 삭제된 공지(USE_AT='N')면 null 이라 404
        String atchFileId = fileMapper.selectNoticeAtchFileId(NoticeService.BBS_ID, nttId);
        FileVO file = StringUtils.hasText(atchFileId) ? fileMngService.selectFileInf(key(atchFileId, fileSn)) : null;
        if (file == null) {
            throw messages.notFound("file.notFound");
        }
        return open(file);
    }

    private Download open(FileVO file) {
        Path path = resolveSafely(file.getFileStreCours(), file.getStreFileNm());
        if (!Files.isRegularFile(path)) {
            throw messages.notFound("file.content.notFound");
        }
        return new Download(file, new FileSystemResource(path));
    }

    private void requireLogin() {
        if (SecurityUtil.getCurrentUserId() == null) {
            throw messages.denied("common.login.required");
        }
    }

    /** 이름·확장자·시그니처 검사. 디스크에는 아직 쓰지 않는다 */
    private CheckedUpload check(MultipartFile multipartFile) {
        if (multipartFile == null || multipartFile.isEmpty()) {
            throw messages.business("file.required");
        }
        String originalName = sanitizeFileName(multipartFile.getOriginalFilename(), messages);
        String ext = extractExtension(originalName);
        List<String> allowed = appProperties.file().allowedExtensions();
        if (!allowed.contains(ext)) {
            throw messages.business("file.extension.denied", String.join(", ", allowed));
        }
        verifySignature(multipartFile, ext);
        return new CheckedUpload(multipartFile, originalName, ext);
    }

    /** 업로드 루트/yyyy/MM 아래에 UUID 이름으로 저장하고 파일 정보(ID·순번 제외)를 만든다 */
    private FileVO writeToDisk(CheckedUpload upload) {
        String subDir = YearMonth.now(clock).format(SUB_DIR_FORMAT);
        String storedName = UUID.randomUUID().toString().replace("-", "");
        Path target = resolveSafely(subDir, storedName);

        try {
            Files.createDirectories(target.getParent());
            try (InputStream in = upload.file().getInputStream()) {
                Files.copy(in, target);
            }
        } catch (IOException e) {
            throw new UncheckedIOException("첨부파일 저장 중 오류가 발생했습니다.", e);
        }
        deleteOnRollback(target);

        FileVO file = new FileVO();
        file.setFileStreCours(subDir);
        file.setStreFileNm(storedName);
        file.setOrignlFileNm(upload.originalName());
        file.setFileExtsn(upload.extension());
        file.setFileMg(Long.toString(upload.file().getSize()));
        return file;
    }

    private static FileVO key(String atchFileId, int fileSn) {
        FileVO key = new FileVO();
        key.setAtchFileId(atchFileId);
        key.setFileSn(Integer.toString(fileSn));
        return key;
    }

    private static long sizeOf(FileVO file) {
        return StringUtils.hasText(file.getFileMg()) ? Long.parseLong(file.getFileMg().trim()) : 0L;
    }

    private String nextAtchFileId() {
        try {
            return fileIdGnrService.getNextStringId();
        } catch (FdlException e) {
            throw new BaseRuntimeException("첨부파일 ID 채번에 실패했습니다.", e);
        }
    }

    /** 경로 구분자/제어문자 제거 및 길이 제한. 원본 파일명은 표시용으로만 쓰고 저장 경로에는 절대 사용하지 않는다 */
    static String sanitizeFileName(String originalFilename, AppMessages messages) {
        if (originalFilename == null) {
            throw messages.business("file.name.invalid");
        }
        // 일부 브라우저는 "C:\Users\...\plan.pdf" 처럼 전체 경로를 보내므로 마지막 이름만 사용
        String name = originalFilename.replace('\\', '/');
        name = name.substring(name.lastIndexOf('/') + 1);
        // 응답 헤더 분할(CRLF) 등에 악용될 수 있는 제어문자 제거
        name = name.replaceAll("[\\p{Cntrl}]", "").trim();
        if (name.isEmpty() || name.equals(".") || name.equals("..")) {
            throw messages.business("file.name.invalid");
        }
        if (name.length() > MAX_ORIGINAL_NAME_LENGTH) {
            String ext = extractExtension(name);
            name = (ext.isEmpty() || ext.length() > 10)
                    ? name.substring(0, MAX_ORIGINAL_NAME_LENGTH)
                    : name.substring(0, MAX_ORIGINAL_NAME_LENGTH - ext.length() - 1) + "." + ext;
        }
        return name;
    }

    static String extractExtension(String fileName) {
        int dot = fileName.lastIndexOf('.');
        if (dot < 0 || dot == fileName.length() - 1) {
            return "";
        }
        return fileName.substring(dot + 1).toLowerCase(Locale.ROOT);
    }

    private void verifySignature(MultipartFile multipartFile, String ext) {
        List<String> signatures = FILE_SIGNATURES.get(ext);
        if (signatures == null) {
            return;
        }
        byte[] header = new byte[8];
        int read;
        try (InputStream in = multipartFile.getInputStream()) {
            read = in.readNBytes(header, 0, header.length);
        } catch (IOException e) {
            throw new UncheckedIOException("첨부파일을 읽는 중 오류가 발생했습니다.", e);
        }
        String headerHex = HexFormat.of().formatHex(Arrays.copyOf(header, read));
        boolean matched = signatures.stream().anyMatch(headerHex::startsWith);
        if (!matched) {
            log.warn("[파일 업로드 차단] 확장자와 실제 파일 형식 불일치 ext={}, user={}", ext, SecurityUtil.getCurrentUserId());
            throw messages.business("file.signature.invalid", ext);
        }
    }

    /** 업로드 루트 기준으로 경로를 만들고, 루트 밖으로 벗어나면 차단 */
    private Path resolveSafely(String subDir, String storedName) {
        Path root = Paths.get(appProperties.file().uploadDir()).toAbsolutePath().normalize();
        Path target = root.resolve(subDir).resolve(storedName).normalize();
        if (!target.startsWith(root)) {
            throw messages.business("file.path.invalid");
        }
        return target;
    }

    /** DB 저장이 실패해 트랜잭션이 롤백되면 이미 디스크에 쓴 파일도 삭제 (고아 파일 방지) */
    private void deleteOnRollback(Path target) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCompletion(int status) {
                if (status == STATUS_ROLLED_BACK) {
                    try {
                        Files.deleteIfExists(target);
                    } catch (IOException e) {
                        log.warn("[파일 정리 실패] {}", target, e);
                    }
                }
            }
        });
    }
}
