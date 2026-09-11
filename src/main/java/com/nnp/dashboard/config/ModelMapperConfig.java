package com.nnp.dashboard.config;

import org.modelmapper.ModelMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import lombok.extern.slf4j.Slf4j;

/**
 * Provides the shared {@link ModelMapper} bean used throughout the services to
 * convert between JPA entities and their Value Objects (VOs / DTOs).
 *
 * @author AC
 */
@Configuration
@Slf4j
public class ModelMapperConfig {
	@Bean(name = "modelMapper")
	public ModelMapper getModelMapper() {
//		log.info("ModelMapperConfig getModelMapper() - Configure ModelMapper");
		return new ModelMapper();
	}

}
