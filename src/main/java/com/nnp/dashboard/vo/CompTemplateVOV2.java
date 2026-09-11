package com.nnp.dashboard.vo;

import com.fasterxml.jackson.annotation.JsonInclude;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

/**
 * VO for a YAML template attached to a component. One component can have
 * multiple templates (env_comp_template). The template content is stored in DB
 * and rendered by the env-replication-engine.
 *
 * @author AC
 */
@Getter
@Setter
@ToString
@JsonInclude(JsonInclude.Include.NON_NULL)
public class CompTemplateVOV2 {
	private String tmplId;
	private String template;
	private String fileName;
	private String filePath;
	private String tmplType;
	private String createdBy;
	private String status;
}
