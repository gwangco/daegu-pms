package kr.go.daegu.pms.user.service;

import kr.go.daegu.pms.cmm.service.ComDefaultVO;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/** 사용자 관리 목록 검색 조건 (searchKeyword: 아이디·이름·소속) */
@Getter
@Setter
public class UserSearchVO extends ComDefaultVO {

    private static final long serialVersionUID = 1L;

    private String role;

    /** Y 이면 지금 잠겨 있는 계정만 */
    private String lockedOnly;

    /** 잠금 판단 기준. 이 시각보다 뒤에 잠긴 계정이 아직 잠금 중이다 (서버가 채우며 화면 값은 받지 않음) */
    private LocalDateTime lockedSince;
}
