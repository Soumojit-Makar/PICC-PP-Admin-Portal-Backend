package com.nnp.dashboard.vo;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class TypeCompVOV2 implements Serializable {

    private String typecompId;
    private String dtlspecId;
    private String scriptLink;

    @Override
    public boolean equals(Object obj) {
        return this.typecompId.equalsIgnoreCase(((TypeCompVOV2) obj).typecompId);
    }

    @Override
    public int hashCode() {
        return this.typecompId.hashCode();
    }


}
