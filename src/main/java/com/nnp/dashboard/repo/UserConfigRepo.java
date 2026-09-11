package com.nnp.dashboard.repo;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.nnp.dashboard.model.UserConfig;

public interface UserConfigRepo extends JpaRepository<UserConfig, String> {
	@Query("SELECT u FROM UserConfig u WHERE u.userId = ?1 and u.envId = ?2")
	List<UserConfig> getUsersByEnv(String userId, String envId);

	@Modifying
	@Query("DELETE  FROM UserConfig u WHERE u.userId =:userId and u.envId =:envId")
	void deleteUserByEnv(@Param("userId")String userId, @Param("envId")String envId);
	
	List<UserConfig> findByUserIdAndEnvIdAndChElmDetailIdIsNotNull(String userId, String envId);

	@Modifying
	@Query("DELETE  FROM UserConfig u WHERE u.feaId =:feaId")
	void deleteUserByFeaId(@Param("feaId")String feaId);

	@Modifying
	@Query("DELETE  FROM UserConfig u WHERE u.elemId =:elemId")
	void deleteUserByElemId(@Param("elemId")String elemId);

	@Modifying
	@Query("DELETE  FROM UserConfig u WHERE u.elmDetailId =:elmDetailId")
	void deleteUserByElmDetailId(@Param("elmDetailId")String elmDetailId);

	@Modifying
	@Query("DELETE  FROM UserConfig u WHERE u.chElmDetailId =:chElmDetailId")
	void deleteUserByChElmDetailId(@Param("chElmDetailId")String chElmDetailId);

}
