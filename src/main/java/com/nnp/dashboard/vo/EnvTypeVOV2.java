package com.nnp.dashboard.vo;

import lombok.*;

import java.io.Serializable;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
//@ToString
public class EnvTypeVOV2 implements Serializable {
    private String envTypeId;
    private String envTypeName;
    private String envTypeDesc;
}
