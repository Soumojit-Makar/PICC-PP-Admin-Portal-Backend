package com.nnp.dashboard.repo;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.nnp.dashboard.model.HostPlan;
import com.nnp.dashboard.vo.PlanVOV2;

@Repository
public interface PlanRepoV2 extends JpaRepository<HostPlan, Long>{

	List<HostPlan> findByHostPlanStatus(String status);

}
