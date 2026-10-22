package kr.go.daegu.pms.announcement.service;

import kr.go.daegu.pms.cmm.service.ComDefaultVO;
import lombok.Getter;
import lombok.Setter;

/** 공고 목록 검색 조건 */
@Getter
@Setter
public class AnnouncementSearchVO extends ComDefaultVO {

    private static final long serialVersionUID = 1L;

    private String fieldCode;
    private String status;
}
