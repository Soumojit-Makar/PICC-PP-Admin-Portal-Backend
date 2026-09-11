/**
 *
 */
package com.nnp.dashboard.controller;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

//import com.hazelcast.org.json.HTTP;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

import com.nnp.dashboard.exception.DashboardConfigException;
import com.nnp.dashboard.service.*;
import com.nnp.dashboard.vo.*;
import com.nnp.dashboard.vo.rm.Issue;
import com.nnp.dashboard.vo.rm.Issue__1;

import lombok.extern.slf4j.Slf4j;

/**
 * REST controller for all environment-related configuration operations.
 *
 * Exposes CRUD (Create/Read/Update/Delete) endpoints under {@code /env} for:
 *   - Environments (V2 / V3 versions backed by different entity models)
 *   - Environment features, feature elements, element details and child (CH)
 *     element details
 *   - Lookups such as env types, activity logs, plans, components, specs and
 *     Keycloak realm (tenant) details
 *   - Cross-system existence checks (GitLab group / Redmine project / Redmine login)
 *
 * The logic is delegated to the service layer:
 *   - {@link EnvWrapperService}  : high-level orchestration of env creation
 *   - {@link EnvironmentServ}     : low-level environment CRUD + master replication
 *   - {@link GitlabService}       : GitLab integration (groups, users)
 *   - {@link RedmineService}      : Redmine integration (projects, users, issues)
 *   - {@link KeycloakService}     : Keycloak realm / user integration
 *
 * @author AC
 */
@RestController
//@CrossOrigin(origins = "*", maxAge = 3600)
@RequestMapping("/env")
@Slf4j
public class EnvironmentController {

    @Autowired
    private EnvWrapperService envWrapperService;
    @Autowired
    private GitlabService gitlabService;
    @Autowired
    private EnvironmentServ envService;
    @Autowired
    private RedmineService redmineService;

    @Autowired
    private KeycloakService keycloakService;

    @Autowired
    private ModelMapper modelMapper;

    @Value("${redmine.support.projectId:1}")
    private Integer redmineSupportProjectId;

    @Value("${redmine.support.subject:Support Request}")
    private String redmineSupportSubject;

    @Value("${redmine.support.priorityId:2}")
    private Integer redmineSupportPriorityId;

    @Value("${redmine.support.trackerId:1}")
    private Integer redmineSupportTrackerId;

    @Value("${redmine.support.description:Support request description}")
    private String redmineSupportDescription;


    //TODO
    @GetMapping(path = "/read/v2")
    public List<EnvironmentVOV2> retriveEnvironmentV2() {
//        log.info("EnvironmentController ->retriveEnvironmentV2()");
        return envService.getAllEnvironmentsV2();
    }

    //TODO
    // Stating Of V3 routes
    @GetMapping(path = "/read/v3")
    public List<EnvironmentVOV3> retriveEnvironmentV3() {
//        log.info("EnvironmentController ->retriveEnvironmentV3()");
        return envService.getAllEnvironmentsV3();
    }

    @PutMapping(path = "update/env/v3/{envId}")
    public EnvironmentVOV3 updateEnvironmentV3(
            @PathVariable String envId,
            @RequestBody EnvironmentVOV3 environmentVOV3
    ){
        return envService.updateEnvironmentV3(envId,environmentVOV3);

    }
    @PostMapping(path = "/create/feature/v3")
    public EnvFeatureVOV3 createFeatureV3(
    @RequestBody EnvFeatureVOV3 envFeatureVOV3
    ){
        return envService.createFeatureV3(envFeatureVOV3);
    }
    @PutMapping(path = "/update/feature/v3/{featureId}")
    public Map<String,String> updateFeatureV3(
            @PathVariable String featureId,
            @RequestBody EnvFeatureVOV3 envFeatureVOV3
    ){
        envService.updateEnvFeatureV3(featureId,envFeatureVOV3);
        return Map.of("message","Environment Feature Updated");
    }
    @DeleteMapping(path = "/delete/feature/v3/{featureId}")
    public Map<String,String> deleteFeatureV3(
            @PathVariable String featureId
    ){
        envService.deleteEnvFeatureV3(featureId);
        return Map.of("message","Environment Feature Deleted");
    }
    @PostMapping(path = "/create/feature/element/v3")
    public FeatureElementVOV3 createFeatureElementV3(
            @RequestBody FeatureElementVOV3 featureElementVOV3
    ){
        return envService.createFeatureElementV3(featureElementVOV3);
    }
    @PutMapping(path = "/update/feature/element/v3/{elementId}")
    public Map<String,String> updateFeatureElementV3(
            @PathVariable String elementId,
            @RequestBody FeatureElementVOV3 featureElementVOV3
    ){
        envService.updateFeatureElementV3(elementId,featureElementVOV3);
        return Map.of("message","Feature Element Update");
    }
    @DeleteMapping(path = "/delete/feature/element/v3/{elementId}")
    public Map<String,String> deleteFeatureElementV3(
            @PathVariable String elementId

    ){
        envService.deleteFeatureElementV3(elementId);
        return Map.of("message","Feature Element Delete");
    }
    @PostMapping(path = "/create/feature/element/details/v3")
    public ElementDetailVOV3 createFeatureElementDetailV3(
          @RequestBody ElementDetailVOV3 elementDetailVOV3
    ){
        return envService.createFeatureElementDetailV3(elementDetailVOV3);
    }
    @PutMapping(path = "/update/feature/element/details/v3/{ementId}")
    public Map<String,String> updateEnvElementDetailV3(
            @PathVariable String ementId,
           @RequestBody ElementDetailVOV3 elementDetailVOV3
    ){
        envService.updateEnvElementDetailV3(ementId,elementDetailVOV3);
        return Map.of("message","Element Detail is Updated ");
    }
    @DeleteMapping(path = "/delete/feature/element/details/v3/{ementId}")
    public Map<String,String> deleteEnvElementDetailV3(
            @PathVariable String ementId
    ){
        envService.deleteEnvElementDetailV3(ementId);
        return Map.of("message","Element Detail is Delete ");
    }
    @PostMapping("/create/feature/element/details/chdetails/v3")
    public CHElementDetailVOV3 createCHElementDetailV3(
            @RequestBody CHElementDetailVOV3 chElementDetailVOV3
    ){
        return envService.createCHElementDetailV3(chElementDetailVOV3);
    }
    @PutMapping(path ="/update/feature/element/details/chdetails/v3/{chelementId}" )
    public Map<String,String> updateCHElementDetailV3(
            @PathVariable String chelementId,
            @RequestBody CHElementDetailVOV3 elementDetailVOV3
    ){
        envService.updateCHElementDetailV3(chelementId,elementDetailVOV3);
        return Map.of("message","Element Detail Child is update ");
    }
    @PutMapping(path ="/delete/feature/element/details/chdetails/v3/{chelementId}" )
    public Map<String,String> deleteCHElementDetailV3(
            @PathVariable String chelementId
    ){
        envService.deleteCHElementDetailV3(chelementId);
        return Map.of("message","Element Detail Child is delete ");
    }
    // Ending Of V3 routes



    //TODO
    @GetMapping(path = "/read/tenants/v2")
    public List<TenantDetails> getAllRealmDetailsV2() {
        return keycloakService.getAllRealmDetails()
                .stream()
                .map(realmDetails -> modelMapper.map(realmDetails, TenantDetails.class))
                .toList();
    }


    //TODO
    @GetMapping(path = "/read/activity/v2/{envId}")
    List<EnvActivityLogVOV2> getActivityLogByEnvIdV2(@PathVariable("envId") String envId) {
        return envService.getActivityLogByEnvIdV2(envId);
    }

    //TODO
    @GetMapping(path = "/check/exists/env/code/v2/{envCode}")
    public Boolean checkIfEnvCodeExistsV2(@PathVariable("envCode") String envCode) {
        return envService.checkIfEnvCodeExistsV2(envCode);
    }

    //TODO
    @PutMapping(path = "/update/env/v2/{envId}")
    public EnvironmentVOV2 updateEnvironmentOnlyV2(@RequestBody EnvironmentVOV2 environmentVOV2, @PathVariable("envId") String envId) {
//        log.info("EnvironmentController ->updateEnvironmentOnlyV2() - " + environmentVOV2);
        EnvironmentVOV2 envReply = null;
        envReply = envService.updateEnvironmentOnlyV2(environmentVOV2, envId);
        return envReply;
    }

    //TODO
    @PostMapping(path = "/create/feature/v2")
    public List<EnvFeatureVOV2> createEnvFeaturesOnlyV2(@RequestBody List<EnvFeatureVOV2> envFeatureVOV2List) {
//        log.info("EnvironmentController ->createEnvFeatureOnly()");
        return envService.createEnvFeaturesOnlyV2(envFeatureVOV2List);

    }

//TODO
    @PutMapping(path = "/update/feature/v2/{feaId}")
    public EnvFeatureVOV2 updateEnvFeaturesOnlyV2(@RequestBody EnvFeatureVOV2 envFeatureVOV2, @PathVariable("feaId") String feaId) {
//        log.info("EnvironmentController ->updateEnvFeaturesOnly()");
        return envService.updateEnvFeaturesOnlyV2(envFeatureVOV2, feaId);

    }
//TODO
    @DeleteMapping(path = "/delete/feature/v2/{feaId}")
    public void deleteEnvFeatureV2(@PathVariable("feaId") String feaId) {
        envService.deleteEnvFeatureV2(feaId);
    }
//TODO
    @PostMapping(path = "/create/feature/element/v2")
    public List<FeatureElementVOV2> createFeatureElementsOnlyV2(@RequestBody List<FeatureElementVOV2> featureElementVOV2List) {
//        log.info("EnvironmentController ->createFeatureElementsOnly()");
        return envService.createFeatureElementsOnlyV2(featureElementVOV2List);

    }
//TODO
    @PutMapping(path = "/update/feature/element/v2/{elementId}")
    public FeatureElementVOV2 updateFeatureElementsOnlyV2(@RequestBody FeatureElementVOV2 featureElementVOV2, @PathVariable("elementId") String elementId) {
//        log.info("EnvironmentController ->updateFeatureElementsOnly()");
        return envService.updateFeatureElementsOnlyV2(featureElementVOV2, elementId);

    }
//TODO
    @DeleteMapping(path = "/delete/feature/element/v2/{elementId}")
    public void deleteFeatureElementV2(@PathVariable("elementId") String elementId) {
//        log.info("EnvironmentController ->deleteFeatureElementV2()");
        envService.deleteFeatureElementV2(elementId);
    }
//TODO
    @PostMapping(path = "/create/feature/element/details/v2")
    public List<ElementDetailVOV2> createFeatureElementDetailsOnlyV2(@RequestBody List<ElementDetailVOV2> elementDetailVOV2List) {
//        log.info("EnvironmentController ->createFeatureElementsOnly()");
        return envService.createFeatureElementDetailsOnlyV2(elementDetailVOV2List);

    }
//TODO
    @PutMapping(path = "/update/feature/element/details/v2/{elementDtlId}")
    public ElementDetailVOV2 updateFeatureElementDetailsOnlyV2(@RequestBody ElementDetailVOV2 elementDetailVOV2, @PathVariable("elementDtlId") String elementDtlId) {
//        log.info("EnvironmentController ->updateFeatureElementDetailsOnly()");
        return envService.updateFeatureElementDetailsOnlyV2(elementDetailVOV2, elementDtlId);

    }
//TODO
    @DeleteMapping(path = "/delete/feature/element/details/v2/{elementDtlId}")
    public void deleteFeatureElementDetailsV2(@PathVariable("elementDtlId") String elementDtlId) {
//        log.info("EnvironmentController ->deleteFeatureElementDetailsV2()");
        envService.deleteFeatureElementDetailsV2(elementDtlId);
    }
//TODO
    @PostMapping(path = "/create/feature/element/details/chdetails/v2")
    public List<CHElementDetailVOV2> createFeatureChildElementDetailsOnlyV2(@RequestBody List<CHElementDetailVOV2> chElementDetailVOV2List) {
//        log.info("EnvironmentController ->createFeatureChildElementDetailsOnly()");
        return envService.createFeatureChildElementDetailsOnlyV2(chElementDetailVOV2List);
    }
//TODO
    @PutMapping(path = "/update/feature/element/details/chdetails/v2/{chElementDtlId}")
    public CHElementDetailVOV2 updateFeatureChildElementDetailsOnlyV2(@RequestBody CHElementDetailVOV2 chElementDetailVOV2, @PathVariable("chElementDtlId") String chElementDtlId) {
//        log.info("EnvironmentController ->updateFeatureChildElementDetailsOnly()");
        return envService.updateFeatureChildElementDetailsOnlyV2(chElementDetailVOV2, chElementDtlId);
    }
//TODO
    @DeleteMapping(path = "/delete/feature/element/details/chdetails/v2/{chElementDtlId}")
    public void deleteFeatureChildElementDetailsV2(@PathVariable("chElementDtlId") String chElementDtlId) {
//        log.info("EnvironmentController ->deleteFeatureChildElementDetailsV2()");
        envService.deleteFeatureChildElementDetailsV2(chElementDtlId);
    }
    
    //For environment creation automation work
    @GetMapping(path="/read/plan")
    public List<PlanVOV2> getAllActivePlans(){
//    	log.info("EnvironmentController ->getAllActivePlans()");
    	return envService.getAllActivePlans("Active");
    }
    
    @GetMapping(path="/read/plan/{planId}/comp")
    public Map<String, List<CompVOV2>> getAllActivePlanComp(@PathVariable long planId){
//    	log.info("EnvironmentController ->getAllActivePlanComp()");
    	return envService.getAllActiveComp(planId,"Active");
    }

    @GetMapping(path="/read/plan/{planId}/accounts")
    public List<String> getAccountsByPlan(@PathVariable long planId){
        return envService.getAccountsByPlan(planId);
    }
    
    @GetMapping(path="/read/comp/{compId}/spec")
    public List<CompSpecVOV2> getAllActiveSpecForComp(@PathVariable String compId){
//    	log.info("EnvironmentController ->getAllActiveSpecForComp()");
    	return envService.getAllActiveCompSpec(compId,"Active");
    }
    /*
     * While user login - account Name has been provided. Account Name is same as environment Code.
     * This service is called on focus change in UI to get the corresponding environment details
     * As subsequent login call in sso-auth service required environment id and tenant name
     */
    @GetMapping(path = "/read/env/{envCode}")
    public EnvironmentVOV3 getEnvByCode(@PathVariable String envCode){
//    	log.info("EnvironmentController ->getAllActiveSpecForComp()");
    	return envService.retriveEnvironmentV3ByEnvCode(envCode);
    }

    // =========================================================================
    // Plan creation for environment creation
    // =========================================================================

    @GetMapping(path = "/read/plan/all")
    public List<PlanCreateVOV2> getAllPlans() {
        return envService.getAllPlans();
    }

    @GetMapping(path = "/read/plan/comp/group/all")
    public List<PlanCompGroupVOV2> getAllPlanCompGroups() {
        return envService.getAllPlanCompGroups();
    }

    @PostMapping(path = "/create/plan")
    public PlanCreateVOV2 createPlan(@RequestBody PlanCreateVOV2 planVO) {
        return envService.createPlan(planVO);
    }

    @PutMapping(path = "/update/plan/{planId}")
    public PlanCreateVOV2 updatePlan(@PathVariable long planId, @RequestBody PlanCreateVOV2 planVO) {
        return envService.updatePlan(planId, planVO);
    }

    @DeleteMapping(path = "/delete/plan/{planId}")
    public Map<String, String> deletePlan(@PathVariable long planId) {
        envService.deletePlan(planId);
        return Map.of("message", "Plan Deactivated");
    }

    @PostMapping(path = "/create/plan/{planId}/comp")
    public PlanCompVOV2 createPlanComp(@PathVariable long planId, @RequestBody PlanCompVOV2 planCompVO) {
        return envService.createPlanComp(planId, planCompVO);
    }

    @PutMapping(path = "/update/plan/{planId}/comp/{hostRegPlanId}")
    public PlanCompVOV2 updatePlanComp(@PathVariable long planId, @PathVariable long hostRegPlanId,
            @RequestBody PlanCompVOV2 planCompVO) {
        return envService.updatePlanComp(hostRegPlanId, planCompVO);
    }

    @DeleteMapping(path = "/delete/plan/{planId}/comp/{hostRegPlanId}")
    public Map<String, String> deletePlanComp(@PathVariable long planId, @PathVariable long hostRegPlanId) {
        envService.deletePlanComp(hostRegPlanId);
        return Map.of("message", "Plan Component Mapping Deactivated");
    }

    // =========================================================================
    // Component creation (env_bbcomp + specs + multiple YAML templates)
    // =========================================================================

    @GetMapping(path = "/read/comp/all")
    public List<CompCreateVOV2> getAllComponents() {
        return envService.getAllComponents();
    }

    @PostMapping(path = "/create/comp")
    public CompCreateVOV2 createComponent(@RequestBody CompCreateVOV2 compVO) {
        return envService.createComponent(compVO);
    }

    @PutMapping(path = "/update/comp/{compId}")
    public CompCreateVOV2 updateComponent(@PathVariable String compId, @RequestBody CompCreateVOV2 compVO) {
        return envService.updateComponent(compId, compVO);
    }

    @DeleteMapping(path = "/delete/comp/{compId}")
    public Map<String, String> deleteComponent(@PathVariable String compId) {
        envService.deleteComponent(compId);
        return Map.of("message", "Component Deactivated");
    }

    @PostMapping(path = "/create/comp/{compId}/spec")
    public CompSpecVOV2 createCompSpec(@PathVariable String compId, @RequestBody CompSpecVOV2 specVO) {
        return envService.createCompSpec(compId, specVO);
    }

    @PutMapping(path = "/update/comp/{compId}/spec/{specId}")
    public CompSpecVOV2 updateCompSpec(@PathVariable String compId, @PathVariable String specId,
            @RequestBody CompSpecVOV2 specVO) {
        return envService.updateCompSpec(specId, specVO);
    }

    @DeleteMapping(path = "/delete/comp/{compId}/spec/{specId}")
    public Map<String, String> deleteCompSpec(@PathVariable String compId, @PathVariable String specId) {
        envService.deleteCompSpec(specId);
        return Map.of("message", "Component Spec Deactivated");
    }

    @GetMapping(path = "/read/comp/{compId}/template")
    public List<CompTemplateVOV2> getCompTemplates(@PathVariable String compId) {
        return envService.getCompTemplates(compId);
    }

    @PostMapping(path = "/create/comp/{compId}/template")
    public CompTemplateVOV2 createCompTemplate(@PathVariable String compId, @RequestBody CompTemplateVOV2 tmplVO) {
        return envService.createCompTemplate(compId, tmplVO);
    }

    @PutMapping(path = "/update/comp/{compId}/template/{tmplId}")
    public CompTemplateVOV2 updateCompTemplate(@PathVariable String compId, @PathVariable String tmplId,
            @RequestBody CompTemplateVOV2 tmplVO) {
        return envService.updateCompTemplate(tmplId, tmplVO);
    }

    @DeleteMapping(path = "/delete/comp/{compId}/template/{tmplId}")
    public Map<String, String> deleteCompTemplate(@PathVariable String compId, @PathVariable String tmplId) {
        envService.deleteCompTemplate(tmplId);
        return Map.of("message", "Component Template Deactivated");
    }

}
