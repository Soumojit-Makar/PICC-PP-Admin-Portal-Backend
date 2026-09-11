package com.nnp.dashboard.repo;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.nnp.dashboard.model.CHElementDetail;
import com.nnp.dashboard.model.ElementDetail;
import com.nnp.dashboard.model.EnvUserAccess;

public interface ChElemDetailRepo extends JpaRepository<CHElementDetail, String> {

	public List<CHElementDetail> findByPrElemDtlAndUserList_UserId(ElementDetail prElemDtl,String userId);
	
	public List<CHElementDetail> findByChElementDtlIdAndElementDtlHome(String elementDtlHome,String home);
}
