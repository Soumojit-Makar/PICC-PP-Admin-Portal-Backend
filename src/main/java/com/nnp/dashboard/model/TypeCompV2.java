package com.nnp.dashboard.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;

@Entity
@Table(name = "nnp_type_comp")
@Getter
@Setter
public class TypeCompV2 implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @Column(name = "typecomp_id", nullable = false)
    private String typecompId;

    @Column(name = "dtlspec_id")
    private String dtlspecId;
    @Column(name = "script_link")
    private String scriptLink;
}
