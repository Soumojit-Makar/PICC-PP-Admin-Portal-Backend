package com.nnp.dashboard.repo;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.nnp.dashboard.model.EnvProxyConfig;

@Repository
public interface EnvProxyConfigRepo extends JpaRepository<EnvProxyConfig, String> {
    List<EnvProxyConfig> findByEnvId(String envId);
    Optional<EnvProxyConfig> findByCompServName(String compServName);
    boolean existsByCompServName(String compServName);
}