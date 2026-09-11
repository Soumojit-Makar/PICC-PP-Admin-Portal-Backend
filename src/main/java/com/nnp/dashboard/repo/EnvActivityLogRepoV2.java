package com.nnp.dashboard.repo;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import com.nnp.dashboard.model.EnvActivityLogV2;

import java.util.List;

public interface EnvActivityLogRepoV2 extends JpaRepository<EnvActivityLogV2, String> {

    List<EnvActivityLogV2> findByEnvId(String envId);

    List<EnvActivityLogV2> findByEnvIdOrderByActDateDesc(String envId);

    Page<EnvActivityLogV2> findByEnvId(String envId, Pageable pageable);
}
