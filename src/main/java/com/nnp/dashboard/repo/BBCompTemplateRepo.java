package com.nnp.dashboard.repo;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.nnp.dashboard.model.BBCompTemplate;

/**
 * Repository for the {@code env_comp_template} table. One component can have
 * multiple YAML templates.
 *
 * @author AC
 */
@Repository
public interface BBCompTemplateRepo extends JpaRepository<BBCompTemplate, String> {

	List<BBCompTemplate> findByBbComponent_compId(String compId);
}
