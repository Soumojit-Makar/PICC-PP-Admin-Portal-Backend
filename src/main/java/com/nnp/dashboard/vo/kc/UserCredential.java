package com.nnp.dashboard.vo.kc;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserCredential {
    @Builder.Default
    private Integer hashIterations = 1;
    private String type;
    private String value;
    private boolean temporary;
}
