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
public class DomainValueVOV2 implements Serializable {

	private String dvId;
	private String domainName;
	private String domainValue;
	@Override
	public boolean equals(Object obj) {
		
		return this.dvId.equalsIgnoreCase(((DomainValueVOV2)obj).dvId);
	}
	
	@Override
	public int hashCode() {
		// TODO Auto-generated method stub
		return this.dvId.hashCode();
	}
	
	
}
