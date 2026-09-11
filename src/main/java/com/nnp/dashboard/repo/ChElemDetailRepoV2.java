package com.nnp.dashboard.repo;

import org.springframework.data.jpa.repository.JpaRepository;

import com.nnp.dashboard.model.CHElementDetail;
import com.nnp.dashboard.model.CHElementDetailV2;
import com.nnp.dashboard.model.ElementDetail;

import java.util.List;

public interface ChElemDetailRepoV2 extends JpaRepository<CHElementDetailV2, String> {

	public List<CHElementDetailV2> findByElementDtlId(String elementDtlId);
}
