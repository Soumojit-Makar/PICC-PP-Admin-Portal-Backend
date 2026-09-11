package com.nnp.dashboard.repo;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.nnp.dashboard.model.RoleV2;

public interface RoleRepoV2 extends JpaRepository<RoleV2, String> {
  public List<RoleV2> findByRoleName(String roleName);

public  List<RoleV2> findByRoleStatus(String string);
}
