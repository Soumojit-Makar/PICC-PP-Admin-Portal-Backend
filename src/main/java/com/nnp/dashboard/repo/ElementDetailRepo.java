package com.nnp.dashboard.repo;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.nnp.dashboard.model.ElementDetail;
import com.nnp.dashboard.model.EnvFeature;
import com.nnp.dashboard.model.EnvUserAccess;
import com.nnp.dashboard.model.FeatureElement;

public interface ElementDetailRepo extends JpaRepository<ElementDetail, String> {

	public List<ElementDetail> findByFeaElementAndUserList_UserId(FeatureElement feaElement,String userId);
}
