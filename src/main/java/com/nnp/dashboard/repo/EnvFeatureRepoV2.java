package com.nnp.dashboard.repo;

import org.springframework.data.jpa.repository.JpaRepository;

import com.nnp.dashboard.model.EnvFeature;
import com.nnp.dashboard.model.EnvFeatureV2;
import com.nnp.dashboard.model.Environment;

import java.util.List;

public interface EnvFeatureRepoV2 extends JpaRepository<EnvFeatureV2, String>{

	public List<EnvFeatureV2> findByEnvId(String envId);
}
