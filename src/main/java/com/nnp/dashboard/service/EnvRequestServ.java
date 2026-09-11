package com.nnp.dashboard.service;

import java.util.List;
import java.util.Optional;

import com.nnp.dashboard.utils.Utils;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.nnp.dashboard.exception.DashboardConfigException;
import com.nnp.dashboard.exception.DashboardConfigExceptionMessage;
import com.nnp.dashboard.model.BBCompSpec;
import com.nnp.dashboard.model.BBComponent;
import com.nnp.dashboard.model.ReqComponent;
import com.nnp.dashboard.model.ReqSpec;
import com.nnp.dashboard.model.Request;
import com.nnp.dashboard.repo.CompRepoV2;
import com.nnp.dashboard.repo.CompSpecRepoV2;
import com.nnp.dashboard.repo.EnvRequestRepoV2;
import com.nnp.dashboard.vo.EnvRequestVOV2;
import com.nnp.dashboard.vo.ReqComponentVOV2;
import com.nnp.dashboard.vo.ReqSpecVOV2;

import lombok.extern.slf4j.Slf4j;
/**
 * Service that persists an "environment creation request" - the DTO assembled
 * by {@link NNPAccountService} after payment confirmation.
 *
 * The request ( {@link EnvRequestVOV2} ) contains the selected plan, components
 * and component-specifications. This service converts the DTO into the entity
 * graph ( {@link Request} -&gt; {@link ReqComponent} -&gt; {@link ReqSpec} ),
 * wiring the JPA associations to the resolved BBComponent / BBCompSpec master
 * data, and stores it. The generated {@code reqId} is later pushed to the JMS
 * queue by {@code LatchEventListener.doFinalActivity} so the replication
 * engine can drive the GitOps deployment.
 *
 * @author AC
 */
@Service
@Slf4j
@Transactional
public class EnvRequestServ {
	
	@Autowired
	private EnvRequestRepoV2 envReqRepoV2;
	
	@Autowired
	private CompRepoV2 compRepoV2;
	
	@Autowired
	private CompSpecRepoV2 compSpecRepoV2;
	
    @Autowired
    private ModelMapper dtoToEntityMapper;
	
	public String createEnvCreationReq(EnvRequestVOV2 envReqVOV2) {
		Request req = createEntityFromDto(envReqVOV2);
		req = envReqRepoV2.saveAndFlush(req);
		
		return String.valueOf(req.getReqId());
	}
	
	private Request createEntityFromDto(EnvRequestVOV2 envReqVOV2) {

		Request request = dtoToEntityMapper.map(envReqVOV2, Request.class);

		List<ReqComponentVOV2> listOfReqCompVOV2 = envReqVOV2.getReqCompVOV2();
		List<ReqComponent> listOfReqComps = request.getReqComp();
		int noOfReqCompVOV2s = listOfReqCompVOV2.size();

		for (int i = 0; i < noOfReqCompVOV2s; i++) {
			ReqComponentVOV2 reqCompVOV2 = listOfReqCompVOV2.get(i);
			String bbCompId = reqCompVOV2.getCompId();
			ReqComponent reqComp = listOfReqComps.get(i);

			// Set BBComp in ReqComp
			Optional<BBComponent> bbComp = compRepoV2.findById(bbCompId);
			if (bbComp.isPresent()) {
				request.getReqComp().get(i).setBbComponent(bbComp.get());
			} else {

				log.error("Data Issue : bbCompId {}  is not found",Utils.sanitizeForLog(bbCompId));
				throw new DashboardConfigException(new DashboardConfigExceptionMessage("409","Data Issue : bbCompId "+Utils.sanitizeForLog(bbCompId)+"is not found"));
			}

			// Set Request in ReqComp
			reqComp.setRequest(request);

			// Set compSpec in reqSpec
			List<ReqSpecVOV2> listOfReqSpecVOV2s = reqCompVOV2.getReqSpec();
			List<ReqSpec> listOfReqSpec = reqComp.getReqSpec();
			int noOfReqSpecVOV2s = listOfReqSpecVOV2s.size();

			for (int j = 0; j < noOfReqSpecVOV2s; j++) {
				String bbCompSpecId = listOfReqSpecVOV2s.get(j).getSpecid();
				ReqSpec reqSpec = listOfReqSpec.get(j);
				Optional<BBCompSpec> bbCompSpec = compSpecRepoV2.findById(bbCompSpecId);
				if (bbCompSpec.isPresent()) {
					reqSpec.setBbCompSpec(bbCompSpec.get());
				} else {
					log.error("Data Issue : bbCompSpecId {} is not found",Utils.sanitizeForLog(bbCompSpecId));
					throw new DashboardConfigException(new DashboardConfigExceptionMessage("409","Data Issue : bbCompSpecId " + Utils.sanitizeForLog(bbCompSpecId) + " is not found"));
				}
				reqSpec.setReqComponent(reqComp);

			}
		}

		return request;
	}

}
