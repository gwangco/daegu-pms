package kr.go.daegu.pms.domain;

import kr.go.daegu.pms.common.vo.SearchVO;
import lombok.Getter;
import lombok.Setter;

/** 공고 목록 검색 조건 */
@Getter
@Setter
public class AnnouncementSearchVO extends SearchVO {
    private String fieldCode;
    private String status;
}
