package com.nnp.dashboard.repo;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.nnp.dashboard.model.NNPPlanCompGroup;

/**
 * Repository for the {@code nnp_plan_comp_group} table. Component groups are
 * reused while mapping components to a plan.
 *
 * @author AC
 */
@Repository
public interface PlanCompGroupRepo extends JpaRepository<NNPPlanCompGroup, Long> {

}
