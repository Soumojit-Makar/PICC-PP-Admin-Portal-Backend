
package com.nnp.dashboard.repo;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.nnp.dashboard.model.Environment;

/**
 * @author AC
 *
 */
@Repository
public interface EnvironmentRepo extends JpaRepository<Environment, String> {

	
	public Environment findByEnvCode(String envCode);

	

}
