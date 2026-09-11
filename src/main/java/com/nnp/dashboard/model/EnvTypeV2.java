package com.nnp.dashboard.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.io.Serializable;

/**
 * @author AC
 */

@Entity

@Table(name = "nnp_env_type")
@Getter
@Setter
//@ToString
public class EnvTypeV2 implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id

    @Column(name = "envtype_id", nullable = false)
    private String envTypeId;

    @Column(name = "envtype_name", nullable = false)
    private String envTypeName;

    @Column(name = "envtype_desc")
    private String envTypeDesc;

}
