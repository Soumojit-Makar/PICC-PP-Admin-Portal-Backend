package com.nnp.dashboard.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EnvActivityLogVOV2 {
    private String actId;
    private String envId;
    private LocalDateTime actDate;
    private String actDesc;
    private String actStatus;
    private String actNote;
    private String userId;
}
