package com.nnp.dashboard.vo;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;
import java.time.LocalDateTime;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class ActionVOV2 implements Serializable {

    private String action;

    @Override
    public boolean equals(Object obj) {
        return this.action.equalsIgnoreCase(((ActionVOV2) obj).action);
    }

    @Override
    public int hashCode() {
        // TODO Auto-generated method stub
        return this.action.hashCode();
    }


}
