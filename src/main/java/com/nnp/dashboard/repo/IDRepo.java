package com.nnp.dashboard.repo;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import com.nnp.dashboard.model.ID;
@Repository
public interface IDRepo extends JpaRepository<ID, Long> {
	
	@Query(value = "SELECT nextval('env_generic_id_seq')", nativeQuery = true)
	public Long getNextSeqVal();


}
