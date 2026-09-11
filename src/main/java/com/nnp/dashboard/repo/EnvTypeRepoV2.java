package com.nnp.dashboard.repo;

import org.springframework.data.jpa.repository.JpaRepository;

import com.nnp.dashboard.model.EnvTypeV2;

import java.util.List;

public interface EnvTypeRepoV2 extends JpaRepository<EnvTypeV2, String>{

	public List<EnvTypeV2> findByEnvTypeName(String envTypeName);
}
