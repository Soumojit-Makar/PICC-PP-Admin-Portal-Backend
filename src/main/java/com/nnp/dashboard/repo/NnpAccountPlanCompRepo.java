package com.nnp.dashboard.repo;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.nnp.dashboard.model.NnpAccountPlanComp;

public interface NnpAccountPlanCompRepo extends JpaRepository<NnpAccountPlanComp, String> {

    // Fetch all plan components for a given account by account name
    List<NnpAccountPlanComp> findByNnpAccountPlan_NnpAccount_AccName(String accName);

}
