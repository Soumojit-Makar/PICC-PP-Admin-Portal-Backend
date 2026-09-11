package com.nnp.dashboard.repo;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.nnp.dashboard.model.HostPlanComp;

public interface PlanCompRepoV2 extends JpaRepository<HostPlanComp, Long> {

	List<HostPlanComp> findByHostPlan_hostPlanid(long planId);
	
	HostPlanComp findByEnvBBComp_compIdAndHostPlan_hostPlanid(String compId, long planId);

}
