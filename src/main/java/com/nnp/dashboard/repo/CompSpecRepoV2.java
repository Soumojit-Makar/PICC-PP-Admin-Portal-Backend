package com.nnp.dashboard.repo;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.nnp.dashboard.model.BBCompSpec;
@Repository
public interface CompSpecRepoV2 extends JpaRepository<BBCompSpec, String> {

}
