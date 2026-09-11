package com.nnp.dashboard.repo;

import org.springframework.data.jpa.repository.JpaRepository;

import com.nnp.dashboard.model.RoleElemV2;

import java.util.List;

public interface RoleElemRepoV2 extends JpaRepository<RoleElemV2, String> {
    public List<RoleElemV2> findByRoleIdAndEnvId(String roleId, String envId);
}
