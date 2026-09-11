package com.nnp.dashboard.repo;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.nnp.dashboard.model.UserConfig;
import com.nnp.dashboard.model.UserV2;

import java.util.List;
import java.util.Optional;

public interface UserRepoV2 extends JpaRepository<UserV2, String> {

    @Query("SELECT u FROM UserV2 u WHERE u.userStatus = :userStatus")
    List<UserV2> getUsersByStatus(@Param("userStatus") String userStatus);

    List<UserV2> findByEnvId(String envId);

    List<UserV2> findByAccId(String accId);

    List<UserV2> findByEnvIdAndUserStatus(String envId, String userStatus);

	UserV2 findByUserId(String logedInUserId);

	List<UserV2> findByEnvIdAndUserId(String envId, String userId);

	List<UserV2> findByEnvIdAndUserStatusAndUserTypeNot(String envId, String string, String string2);
	
	UserV2 findByAccIdAndUserTypeAndUserStatus(String accId,String userType,String status);

    @Query("SELECT u FROM UserV2 u WHERE u.userId = :userIdentifier OR u.emailId = :userIdentifier")
    List<UserV2> findByUserIdOrUserEmail(@Param("userIdentifier") String userIdentifier);
}
