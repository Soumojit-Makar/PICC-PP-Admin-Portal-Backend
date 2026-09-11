package com.nnp.dashboard.repo;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import com.nnp.dashboard.model.NnpAccount;

import jakarta.transaction.Transactional;

public interface NnpAccountRepo extends JpaRepository<NnpAccount, String> {

	Optional<NnpAccount> findByAccName(String accName);

	@Modifying
	@Transactional
	@Query("DELETE FROM NnpAccount ac WHERE ac.accId = ?1")
	void deleteByAccount(String accId);
	
	@Modifying
	@Transactional
	@Query("UPDATE NnpAccount ac SET ac.env = ?1 WHERE ac.accId = ?2")
	void updateAccountWithEnv(String envId, String accId);
	

}
