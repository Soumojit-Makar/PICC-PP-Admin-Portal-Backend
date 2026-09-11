package com.nnp.dashboard.repo;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.nnp.dashboard.model.BBComponent;
@Repository
public interface CompRepoV2 extends JpaRepository<BBComponent, String> {

}
