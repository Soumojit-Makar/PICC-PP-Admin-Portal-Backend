package com.nnp.dashboard.repo;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.nnp.dashboard.model.EnvFeature;
import com.nnp.dashboard.model.EnvUserAccess;
import com.nnp.dashboard.model.Environment;

public interface EnvFeatureRepo extends JpaRepository<EnvFeature, String>{

	public List<EnvFeature> findByEnvAndUserList_UserId(Environment env,String userId);
}
