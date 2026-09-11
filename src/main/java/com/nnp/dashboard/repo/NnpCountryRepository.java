package com.nnp.dashboard.repo;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.nnp.dashboard.model.NnpCountry;

@Repository
public interface NnpCountryRepository extends JpaRepository<NnpCountry, String> {

	Optional<NnpCountry> findByCountryCode(String countryCode);
	
	 
}
