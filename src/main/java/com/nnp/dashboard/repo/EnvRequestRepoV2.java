package com.nnp.dashboard.repo;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.nnp.dashboard.model.Request;
@Repository
public interface EnvRequestRepoV2 extends JpaRepository<Request, String> {

}
