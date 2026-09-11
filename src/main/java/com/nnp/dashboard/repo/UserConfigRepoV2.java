package com.nnp.dashboard.repo;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.nnp.dashboard.model.UserConfig;
import com.nnp.dashboard.model.UserConfigV2;

import java.util.List;

public interface UserConfigRepoV2 extends JpaRepository<UserConfigV2, String> {
	@Query("SELECT u FROM UserConfigV2 u WHERE u.userId = ?1 and u.envId = ?2")
	List<UserConfigV2> getUsersByEnv(String userId, String envId);

	@Modifying
	@Query("DELETE  FROM UserConfigV2 u WHERE u.userId =:userId and u.envId =:envId")
	void deleteUserByEnv(@Param("userId")String userId, @Param("envId")String envId);
	
	List<UserConfigV2> findByUserIdAndEnvIdAndChElmDetailIdIsNotNull(String userId, String envId);

	@Modifying
	@Query("DELETE  FROM UserConfigV2 u WHERE u.chElmDetailId =:chElmDetailId")
	void deleteUserByChElmDetailId(@Param("chElmDetailId")String chElmDetailId);

	
}
