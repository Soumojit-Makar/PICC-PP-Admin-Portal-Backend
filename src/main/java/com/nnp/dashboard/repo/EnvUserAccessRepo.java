package com.nnp.dashboard.repo;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import com.nnp.dashboard.model.EnvUserAccess;

public interface EnvUserAccessRepo extends JpaRepository<EnvUserAccess, String> {
	@Query(value = "select * from nnp_env_user_access u inner join nnp_env e on u.env_id=e.env_id where e.env_id =? ", nativeQuery = true )
	public List<EnvUserAccess> getUsers(String envId);
	
	public List<EnvUserAccess> findByUserId(String userId);

}
