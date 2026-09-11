
package com.nnp.dashboard.repo;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.nnp.dashboard.model.Environment;
import com.nnp.dashboard.model.EnvironmentV2;

import java.util.Optional;

/**
 * @author AC
 *
 */
@Repository
public interface EnvironmentRepoV2 extends JpaRepository<EnvironmentV2, String> {
	public Optional<EnvironmentV2> findByEnvCode(String envCode);

	public Boolean existsByEnvCode(String envCode);

	public Optional<EnvironmentV2> findByEnvTypeId(String envTypeId);

}
