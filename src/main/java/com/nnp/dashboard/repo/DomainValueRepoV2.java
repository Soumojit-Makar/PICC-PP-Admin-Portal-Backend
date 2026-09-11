package com.nnp.dashboard.repo;

import org.springframework.data.jpa.repository.JpaRepository;

import com.nnp.dashboard.model.DomainValueV2;

import java.util.List;

public interface DomainValueRepoV2 extends JpaRepository<DomainValueV2, String> {
  public List<DomainValueV2> findByDomainName(String domainName);
}
