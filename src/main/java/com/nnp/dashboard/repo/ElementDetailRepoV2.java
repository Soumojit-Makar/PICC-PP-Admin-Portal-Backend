package com.nnp.dashboard.repo;

import org.springframework.data.jpa.repository.JpaRepository;

import com.nnp.dashboard.model.ElementDetail;
import com.nnp.dashboard.model.ElementDetailV2;
import com.nnp.dashboard.model.FeatureElement;

import java.util.List;

public interface ElementDetailRepoV2 extends JpaRepository<ElementDetailV2, String> {

	public List<ElementDetailV2> findByElementId(String elementId);
}
