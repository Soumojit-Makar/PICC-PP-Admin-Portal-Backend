package com.nnp.dashboard.vo;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserCredentialVOV2 {
    private String type;
    private String oldValue;
    private String value;
    private boolean temporary;
}
