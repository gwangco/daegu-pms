package kr.go.daegu.pms.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CommonCode {
    private String groupId;
    private String groupName;
    private String codeId;
    private String codeName;
    private int sortOrder;
    private String useYn;
}
