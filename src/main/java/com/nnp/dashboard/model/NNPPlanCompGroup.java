package com.nnp.dashboard.model;

import java.io.Serializable;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "nnp_plan_comp_group")
@Getter
@Setter
public class NNPPlanCompGroup implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	@Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "cn_hosting_generic_id_seq")
    @SequenceGenerator(name = "cn_hosting_generic_id_seq", sequenceName = "cn_hosting_generic_id_seq", allocationSize = 1)
    @Column(name = "comp_group_id")
    private long hostPlanCompGroupId;

    @Column(name = "comp_group_title")
    private String compGroupTitle;

    @Column(name = "comp_group_desc")
    private String compGroupDesc;
    
    @Column(name = "item_seq")
    private int itemSeq;

}
