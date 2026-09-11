
package com.nnp.dashboard.repo;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.stereotype.Repository;
import com.nnp.dashboard.model.EnvironmentV3;

import java.util.List;
import java.util.Optional;

/**
 * @author AC
 *
 */
@Repository
public interface EnvironmentRepoV3 extends JpaRepository<EnvironmentV3, String> {
	public Optional<EnvironmentV3> findByEnvCode(String envCode);

	@EntityGraph(attributePaths = { "envFeatures" })
	List<EnvironmentV3> findAll();
}
