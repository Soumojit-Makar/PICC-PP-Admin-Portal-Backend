package com.nnp.dashboard.vo;

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

/**
 * VO used to create / update a component (env_bbcomp) together with its
 * optional list of specs (env_compspec) and YAML templates
 * (env_comp_template).
 *
 * @author AC
 */
@Getter
@Setter
@ToString
@JsonInclude(JsonInclude.Include.NON_NULL)
public class CompCreateVOV2 {
	private String compId;
	private String compName;
	private String compDesc;
	private String compStatus;
	private String compType;
	private Short itemSeq;
	private String envPlatform;
	private String envGitPath;
	private String envGitToken;
	private String sgaredCompServUrl;
	private String k8sCompName;
	private boolean isProxyExpose;
	private List<CompSpecVOV2> specs = new ArrayList<CompSpecVOV2>();
	private List<CompTemplateVOV2> templates = new ArrayList<CompTemplateVOV2>();
}
