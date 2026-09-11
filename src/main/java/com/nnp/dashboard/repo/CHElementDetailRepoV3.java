package com.nnp.dashboard.repo;

import com.nnp.dashboard.model.CHElementDetailV3;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CHElementDetailRepoV3 extends JpaRepository<CHElementDetailV3,String> {
}
