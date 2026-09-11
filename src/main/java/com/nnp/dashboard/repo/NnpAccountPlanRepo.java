package com.nnp.dashboard.repo;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import com.nnp.dashboard.model.NnpAccountPlan;

public interface NnpAccountPlanRepo extends JpaRepository<NnpAccountPlan, String> {
    List<NnpAccountPlan> findByNnpPlan_HostPlanid(long hostPlanid);
}
