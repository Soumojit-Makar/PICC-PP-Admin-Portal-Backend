package com.nnp.dashboard.service;

import java.security.SecureRandom;
import java.util.*;
import java.util.stream.Collectors;

import com.nnp.dashboard.utils.Utils;
import org.apache.commons.lang3.SerializationUtils;
import org.apache.commons.lang3.StringUtils;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.nnp.dashboard.exception.DashboardConfigException;
import com.nnp.dashboard.exception.DashboardConfigExceptionMessage;
import com.nnp.dashboard.model.*;
import com.nnp.dashboard.repo.*;
import com.nnp.dashboard.vo.*;

import lombok.extern.slf4j.Slf4j;

/**
 * Core service for environment configuration and its hierarchical model.
 *
 * Responsibilities:
 *   - CRUD for environments (V2 / V3 data models)
 *   - CRUD for the per-environment hierarchy:
 *       Environment -> EnvFeature -> FeatureElement -> ElementDetail -> CHElementDetail
 *   - "Master" environment cloning: a new environment is created by deep-copying
 *     the seeded *master* environment and re-Keying every id with the new env code
 *     (see {@link #createNewEnvironmentV2}). Because this is a template-driven
 *     approach, only the CHElementDetails matching the customer's selected
 *     components (plus the "common" component) are copied.
 *   - Populating the master nnp_user_role_elem rows that map each role to the
 *     child-element access points of the new environment.
 *   - Plan / component / spec look-ups used by the environment creation wizard.
 *
 * All write-methods run inside a Spring transaction ({@code @Transactional}).
 *
 * @author AC
 */
@Service
@Slf4j
@Transactional
public class EnvironmentServ {
    @Autowired
    private EnvTypeRepoV2 envTypeRepoV2;
    @Autowired
    private EnvFeatureRepo envFeatureRepo;
    @Autowired
    private EnvFeatureRepoV2 envFeatureRepoV2;
    @Autowired
    private FeatureElementRepoV2 featureElementRepoV2;
    @Autowired
    private ElementDetailRepoV2 elementDetailRepoV2;
    @Autowired
    private ChElemDetailRepoV2 chElemDetailRepoV2;
    @Autowired
    private EnvironmentRepo envRepository;
    @Autowired
    private EnvironmentRepoV2 envRepositoryV2;
    @Autowired
    private EnvironmentRepoV3 envRepositoryV3;
    @Autowired
    private IDRepo idRepo;
    @Autowired
    private ModelMapper modelMapper;
    @Autowired
    private UserConfigRepoV2 userConfigRepoV2;
    @Autowired
    private RoleElemRepoV2 roleElemRepoV2;
    @Autowired
    private RoleRepoV2 roleRepoV2;
    @Autowired
    private EnvActivityLogService envActivityLogService;
    @Autowired
    private TypeCompRepoV2 typeCompRepoV2;
    @Autowired
    private PlanRepoV2 planRepoV2;
    @Autowired
    private PlanCompRepoV2 planCompRepoV2;
    @Autowired
    private CompRepoV2 compRepoV2;
    @Autowired
    private NnpAccountRepo accRepo;
    @Autowired
    private EnvFeatureRepoV3 envFeatureRepoV3;
    @Autowired
    private FeatureElementRepoV3 featureElementRepoV3;
    @Autowired
    private ElementDetailRepoV3 elementDetailRepoV3;
    @Autowired
    private CHElementDetailRepoV3 chElementDetailRepoV3;
    @Autowired
    private UserRepoV2 userRepoV2;
    @Autowired
    private BBCompTemplateRepo bbCompTemplateRepo;
    @Autowired
    private PlanCompGroupRepo planCompGroupRepo;
    @Autowired
    private CompSpecRepoV2 compSpecRepoV2;
    @Autowired
    private NnpCountryRepository nnpCountryRepo;
    @Autowired
    private NnpAccountPlanRepo nnpAccountPlanRepo;
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    public List<String> getAccountsByPlan(long planId) {
        return nnpAccountPlanRepo.findByNnpPlan_HostPlanid(planId).stream()
                .map(accPlan -> accPlan.getNnpAccount().getAccName())
                .collect(Collectors.toList());
    }

    public List<EnvActivityLogVOV2> getActivityLogByEnvIdV2(String envId) {
        return envActivityLogService.getActivityLogByEnvId(envId);
    }

    public Page<EnvActivityLogVOV2> getActivityLogByEnvIdWithPaging(String envId, int pageNumber, int size) {
        return envActivityLogService.getActivityLogByEnvIdWithPaging(envId, pageNumber, size);
    }

    public void writeActivityLogV2(EnvActivityLogVOV2 envActivityLogVOV2) {
        envActivityLogService.logActivity(envActivityLogVOV2);
    }

    public List<EnvTypeVOV2> findAllEnvTypeV2() {
        return envTypeRepoV2.findAll()
                .stream()
                .map(envTypeV2 -> modelMapper.map(envTypeV2, EnvTypeVOV2.class))
                .toList();
    }

    public EnvTypeVOV2 findEnvTypeByTypeIdV2(String envTypeId) {
        return envTypeRepoV2.findById(envTypeId)
                .stream()
                .map(envTypeV2 -> modelMapper.map(envTypeV2, EnvTypeVOV2.class))
                .findFirst()
                .orElseThrow(() -> new DashboardConfigException(
                        new DashboardConfigExceptionMessage("404", "Env type not found with type id " + envTypeId)));
    }

    public EnvTypeVOV2 findEnvTypeByTypeNameV2(String envTypeName) {
        return envTypeRepoV2.findByEnvTypeName(envTypeName)
                .stream()
                .map(envTypeV2 -> modelMapper.map(envTypeV2, EnvTypeVOV2.class))
                .findFirst()
                .orElseThrow(() -> new DashboardConfigException(new DashboardConfigExceptionMessage("404",
                        "Env type not found with type name " + envTypeName)));
    }

    private EnvironmentV2 getMasterEnvV2() {
        Optional<EnvironmentV2> master = envRepositoryV2
                .findByEnvTypeId(findEnvTypeByTypeNameV2("master").getEnvTypeId());
        if (master.isEmpty())
            throw new DashboardConfigException(
                    new DashboardConfigExceptionMessage("404", "Master Env data not present in the system"));
        return master.get();
    }

    private List<EnvFeatureV2> getMasterEnvFeatureV2(String envId) {
        return envFeatureRepoV2.findByEnvId(envId);
    }

    private List<FeatureElementV2> getMasterFeatureElementV2(List<EnvFeatureV2> envFeatureV2List) {
        return envFeatureV2List.stream()
                .map(envFeatureV2 -> featureElementRepoV2.findByFeatureId(envFeatureV2.getFeaId()))
                .flatMap(Collection::stream)
                .toList();

    }

    private List<ElementDetailV2> getMasterElementDetailV2(List<FeatureElementV2> featureElementV2List) {
        return featureElementV2List.stream()
                .map(featureElementV2 -> elementDetailRepoV2.findByElementId(featureElementV2.getElementId()))
                .flatMap(Collection::stream)
                .toList();
    }

    private List<CHElementDetailV2> getMasterCHElementDetailV2(List<ElementDetailV2> elementDetailV2List) {
        return elementDetailV2List.stream()
                .map(elementDetailV2 -> chElemDetailRepoV2.findByElementDtlId(elementDetailV2.getElementDtlId()))
                .flatMap(Collection::stream)
                .toList();
    }

    private String getId(String masterId, String envCode) {
        return envCode + masterId.substring(masterId.indexOf("_"));
    }

    public EnvironmentVOV2 createNewEnvironmentV2(EnvironmentVOV2 environmentVOV2, List<String> selectedCompIds,
            EnvActivityLogVOV2 envActivityLogVOV2, NnpAccount account) {
//        log.info("EnvironmentServ -> createNewEnvironmentV2() - " + environmentVOV2);
        EnvironmentV2 environmentV2 = modelMapper.map(environmentVOV2, EnvironmentV2.class);
        // add common with the selectedCompIds
        selectedCompIds.add("common");
        environmentV2.setEnvId(environmentV2.getEnvCode());

        if (StringUtils.isBlank(environmentV2.getEnvStatus())) {
            environmentV2.setEnvStatus("active");
        }
        if (StringUtils.isBlank(environmentV2.getEnvNamespace())) {
            environmentV2.setEnvNamespace(environmentV2.getEnvCode());
        }
        if (StringUtils.isBlank(environmentV2.getEnvRepo())) {
            environmentV2.setEnvRepo(environmentV2.getEnvCode());
        }
        if (account != null) {
            if (StringUtils.isBlank(environmentV2.getEnvCustId())) {
                environmentV2.setEnvCustId(account.getCreatedBy());
            }
            if (StringUtils.isBlank(environmentV2.getEnvCustName())) {
                environmentV2.setEnvCustName(account.getAccName() + " Admin");
            }
        }
        if (StringUtils.isBlank(environmentV2.getEnvEmail()) && StringUtils.isNotBlank(environmentVOV2.getEnvEmail())) {
            environmentV2.setEnvEmail(environmentVOV2.getEnvEmail());
        }
        if (StringUtils.isBlank(environmentV2.getEnvTenantId())) {
            environmentV2.setEnvTenantId(environmentV2.getEnvCode());
        }

        // -- create env --//
        EnvironmentV2 masterEnvironmentV2 = getMasterEnvV2();
//        log.info("masterEnvironmentV2 --> {}", masterEnvironmentV2);
//        log.info("environmentV2 --> {}", environmentV2);
        EnvironmentV2 retEnvV2 = envRepositoryV2.saveAndFlush(environmentV2);

        // Update Account with new environment id
        if (retEnvV2 != null
                && retEnvV2.getEnvId() != null
                && account != null
                && account.getAccId() != null) {

            accRepo.updateAccountWithEnv(
                    retEnvV2.getEnvId(),
                    account.getAccId()
            );
        } else {
            log.error("Cannot update account environment: retEnvV2 or account data is null");
        }

        envActivityLogVOV2.setEnvId(retEnvV2.getEnvId());
        envActivityLogVOV2.setActDesc("env creation in db");
        envActivityLogVOV2.setActNote("env-creation-db");
        envActivityLogVOV2.setActStatus("start");
        envActivityLogService.logActivity(envActivityLogVOV2);

        // -- create env features --//
        List<EnvFeatureV2> masterEnvFeatureV2List = getMasterEnvFeatureV2(masterEnvironmentV2.getEnvId()); // -- master
                                                                                                           // env
                                                                                                           // features--//
        List<EnvFeatureV2> envFeatureV2List = masterEnvFeatureV2List.stream()
                .map(SerializationUtils::clone)
                .toList();
        envFeatureV2List.forEach(envFeatureV2 -> {
            envFeatureV2.setEnvId(environmentV2.getEnvCode());
            envFeatureV2.setFeaId(getId(envFeatureV2.getFeaId(), environmentV2.getEnvCode()));
        });
        masterEnvFeatureV2List
                .forEach(envFeatureVOV2 -> log.debug("masterEnvFeatureV2List --> envFeatureV2 --> {}", Utils.sanitizeForLog(envFeatureVOV2)));
        envFeatureV2List
                .forEach(envFeatureVOV2 -> log.debug("envFeatureV2List --> envFeatureV2 --> {}", Utils.sanitizeForLog(envFeatureVOV2)));
        envFeatureRepoV2.saveAllAndFlush(envFeatureV2List);

        // -- create feature elements --//
        List<FeatureElementV2> masterFeatureElementV2List = getMasterFeatureElementV2(masterEnvFeatureV2List); // --
                                                                                                               // master
                                                                                                               // feature
                                                                                                               // elements--//
        List<FeatureElementV2> featureElementV2List = masterFeatureElementV2List.stream()
                .map(SerializationUtils::clone)
                .toList();
        featureElementV2List.forEach(featureElementV2 -> {
            featureElementV2.setFeatureId(getId(featureElementV2.getFeatureId(), environmentV2.getEnvCode()));
            featureElementV2.setElementId(getId(featureElementV2.getElementId(), environmentV2.getEnvCode()));
        });
//        masterFeatureElementV2List.forEach(featureElementV2 -> log
//                .debug("masterFeatureElementV2List --> featureElementV2 --> {}", featureElementV2));
//        featureElementV2List.forEach(
//                featureElementV2 -> log.debug("featureElementV2List --> featureElementV2 --> {}", featureElementV2));
        featureElementRepoV2.saveAllAndFlush(featureElementV2List);

        // -- create element details --//
        List<ElementDetailV2> masterElementDetailV2List = getMasterElementDetailV2(masterFeatureElementV2List); // --
                                                                                                                // master
                                                                                                                // element
                                                                                                                // details--//
        List<ElementDetailV2> elementDetailV2List = masterElementDetailV2List.stream()
                .map(SerializationUtils::clone)
                .toList();
        elementDetailV2List.forEach(elementDetailV2 -> {
            elementDetailV2.setElementId(getId(elementDetailV2.getElementId(), environmentV2.getEnvCode()));
            elementDetailV2.setElementDtlId(getId(elementDetailV2.getElementDtlId(), environmentV2.getEnvCode()));
        });
        masterElementDetailV2List.forEach(
                elementDetailV2 -> log.debug("masterElementDetailV2List --> elementDetailV2 --> {}", Utils.sanitizeForLog(elementDetailV2)));
        elementDetailV2List.forEach(
                elementDetailV2 -> log.debug("elementDetailV2List --> elementDetailV2 --> {}", Utils.sanitizeForLog(elementDetailV2)));
        elementDetailRepoV2.saveAllAndFlush(elementDetailV2List);

        // -- create ch element details --//
        // Filter masterChild elements based on the selected component and common
        // component only
        List<CHElementDetailV2> masterCHElementDetailV2List = getMasterCHElementDetailV2(masterElementDetailV2List); // --
                                                                                                                     // master
                                                                                                                     // ch
                                                                                                                     // element
                                                                                                                     // details--//
        List<CHElementDetailV2> chElementDetailV2List = masterCHElementDetailV2List.stream()
                .filter(masterChildElem -> selectedCompIds.contains(masterChildElem.getComponenetType()))
                .map(SerializationUtils::clone)
                .toList();
        chElementDetailV2List.forEach(chElementDetailV2 -> {
            chElementDetailV2.setElementDtlId(getId(chElementDetailV2.getElementDtlId(), environmentV2.getEnvCode()));
            chElementDetailV2
                    .setChElementDtlId(getId(chElementDetailV2.getChElementDtlId(), environmentV2.getEnvCode()));
            if (!"common".equalsIgnoreCase(chElementDetailV2.getComponenetType())) {
                // for common application no change is required.
                String url = chElementDetailV2.getElementDtlURL();
                chElementDetailV2.setElementDtlURL(url.replace("${env}", environmentVOV2.getEnvCode()));
            }

        });
//        masterCHElementDetailV2List.forEach(chElementDetailV2 -> log
//                .debug("masterCHElementDetailV2List --> chElementDetailV2 --> {}", chElementDetailV2));
//        chElementDetailV2List.forEach(chElementDetailV2 -> log
//                .debug("chElementDetailV2List --> chElementDetailV2 --> {}", chElementDetailV2));
        chElemDetailRepoV2.saveAllAndFlush(chElementDetailV2List);

        envActivityLogVOV2.setActDesc("env creation in db");
        envActivityLogVOV2.setActNote("env-creation-db");
        envActivityLogVOV2.setActStatus("complete");
        envActivityLogService.logActivity(envActivityLogVOV2);

        // -- populate nnp_user_role_elem master table --//
        envActivityLogVOV2.setActDesc("user role master creation in db");
        envActivityLogVOV2.setActNote("role-master-creation-db");
        envActivityLogVOV2.setActStatus("start");
        envActivityLogService.logActivity(envActivityLogVOV2);

        List<RoleV2> roleV2List = roleRepoV2.findAll();
        List<RoleElemV2> roleElemV2List = new ArrayList<>();
        roleV2List.forEach(roleV2 -> {
            chElementDetailV2List.forEach(chElementDetailV2 -> {
                RoleElemV2 roleElemV2 = RoleElemV2.builder()
                        .roleElemId("RELM_" + idRepo.getNextSeqVal())
                        .roleId(roleV2.getRoleId())
                        .chElmDetailId(chElementDetailV2.getChElementDtlId())
                        .envId(environmentV2.getEnvId())
                        .build();
                roleElemV2List.add(roleElemV2);
            });
        });
        roleElemV2List.forEach(roleElemV2 -> log.debug("roleElemV2List --> roleElemV2 --> {}", Utils.sanitizeForLog(roleElemV2)));
        roleElemRepoV2.saveAllAndFlush(roleElemV2List);

        envActivityLogVOV2.setActDesc("user role master creation in db");
        envActivityLogVOV2.setActNote("role-master-creation-db");
        envActivityLogVOV2.setActStatus("complete");
        envActivityLogService.logActivity(envActivityLogVOV2);

        return modelMapper.map(retEnvV2, EnvironmentVOV2.class);
        // return modelMapper.map(environmentV2, EnvironmentVOV2.class);
    }

    public EnvironmentVOV2 createEnvironmentOnlyV2(EnvironmentVOV2 environmentVOV2) {
//        log.info("EnvironmentServ ->createEnvironmentOnlyV2() - " + environmentVOV2);
        EnvironmentV2 environmentV2 = modelMapper.map(environmentVOV2, EnvironmentV2.class);
        if (checkIfEnvCodeExistsV2(environmentV2.getEnvCode())) {
            throw new DashboardConfigException(
                    new DashboardConfigExceptionMessage("409", "Env code is existing in the system"));
        } else {
//            log.debug("env --> {}", environmentV2.toString());

            if (StringUtils.isBlank(environmentV2.getEnvId())) {
                environmentV2.setEnvId(environmentV2.getEnvCode() + "_ENV_" + idRepo.getNextSeqVal());
            }

            environmentV2 = envRepositoryV2.saveAndFlush(environmentV2);
        }

        return modelMapper.map(environmentV2, EnvironmentVOV2.class);

    }

    /*
     * While Account Registration this method is used to check if any environment
     * having same account name exist.
     * NOTE: Assumption is accName is same as environmentCode and both should be
     * unique.
     */
    public Boolean checkIfEnvCodeExistsV2(String envCode) {
        return envRepositoryV2.existsByEnvCode(envCode);
    }

    public EnvironmentVOV2 updateEnvironmentOnlyV2(EnvironmentVOV2 environmentVOV2, String envId) {
//        log.info("EnvironmentServ ->updateEnvironmentOnlyV2() - " + environmentVOV2);
        EnvironmentV2 environmentV2 = modelMapper.map(environmentVOV2, EnvironmentV2.class);

//        log.debug("env --> {}", environmentV2.toString());

        if (StringUtils.isBlank(environmentV2.getEnvId())) {
            environmentV2.setEnvId(envId);
        }

        environmentV2 = envRepositoryV2.saveAndFlush(environmentV2);

        return modelMapper.map(environmentV2, EnvironmentVOV2.class);
    }
    public List<EnvFeatureVOV2> createEnvFeaturesOnlyV2(List<EnvFeatureVOV2> envFeatureVOList) {
//        log.info("EnvironmentServ ->createEnvFeatureOnly()");

        List<EnvFeatureV2> envFeatureV2List = new ArrayList<>();
        for (EnvFeatureVOV2 envFeatureVOV2 : envFeatureVOList) {
//            log.debug("envFeatureVOV2 --> {}", envFeatureVOV2.toString());
            EnvFeatureV2 envFeatureV2 = modelMapper.map(envFeatureVOV2, EnvFeatureV2.class);

            if (StringUtils.isBlank(envFeatureV2.getFeaId())) {
                envFeatureV2.setFeaId("FEA_" + idRepo.getNextSeqVal());
            }
//            log.debug("envFeatureV2 --> {}", envFeatureV2.toString());
            envFeatureV2List.add(envFeatureV2);
        }

        List<EnvFeatureV2> envFeatureV2ResponseList = envFeatureRepoV2.saveAllAndFlush(envFeatureV2List);

        return envFeatureV2ResponseList.stream()
                .map(envFeatureV2 -> modelMapper.map(envFeatureV2, EnvFeatureVOV2.class))
                .collect(Collectors.toList());
    }

    public EnvFeatureVOV2 updateEnvFeaturesOnlyV2(EnvFeatureVOV2 envFeatureVOV2, String feaId) {
//        log.info("EnvironmentServ ->updateEnvFeaturesOnly()");
//
//        log.debug("envFeatureVOV2 --> {}", envFeatureVOV2.toString());
        EnvFeatureV2 envFeatureV2 = modelMapper.map(envFeatureVOV2, EnvFeatureV2.class);

        if (StringUtils.isBlank(envFeatureV2.getFeaId())) {
            envFeatureV2.setFeaId(feaId);
        }
//        log.debug("envFeatureV2 --> {}", envFeatureV2.toString());
        EnvFeatureV2 envFeatureVOV2Res = envFeatureRepoV2.saveAndFlush(envFeatureV2);
        return modelMapper.map(envFeatureVOV2Res, EnvFeatureVOV2.class);

    }

    public void deleteEnvFeatureV2(String feaId) {
        Optional<EnvFeatureV2> envFeatureV2Optional = envFeatureRepoV2.findById(feaId);
//        log.debug("deleteEnvFeature() --> envFeatureV2Optional --> {}", envFeatureV2Optional);
        if (envFeatureV2Optional.isPresent()) {
            EnvFeatureV2 envFeatureV2 = envFeatureV2Optional.get();
//            log.debug("deleteEnvFeature() --> envFeatureV2 --> {}", envFeatureV2);
            List<FeatureElementV2> byFeatureId = featureElementRepoV2.findByFeatureId(envFeatureV2.getFeaId());
            for (FeatureElementV2 featureElementV2 : byFeatureId) {
//                log.debug("deleteEnvFeature() --> featureElementV2 --> {}", featureElementV2);
                if (featureElementV2 != null) {
                    List<ElementDetailV2> byElementId = elementDetailRepoV2
                            .findByElementId(featureElementV2.getElementId());
                    for (ElementDetailV2 elementDetailV2 : byElementId) {
//                        log.debug("deleteEnvFeature() --> elementDetailV2 --> {}", elementDetailV2);
                        if (elementDetailV2 != null) {
                            List<CHElementDetailV2> byElementDtlId = chElemDetailRepoV2
                                    .findByElementDtlId(elementDetailV2.getElementDtlId());
                            for (CHElementDetailV2 chElementDetailV2 : byElementDtlId) {
//                                log.debug("deleteEnvFeature() --> chElementDetailV2 --> {}", chElementDetailV2);
                                if (chElementDetailV2 != null) {
//                                    log.debug("deleteEnvFeature() --> chElementDtlId --> {}",
//                                            chElementDetailV2.getChElementDtlId());
                                    chElemDetailRepoV2.delete(chElementDetailV2);
                                    userConfigRepoV2.deleteUserByChElmDetailId(chElementDetailV2.getChElementDtlId());
                                }
                            }
                            elementDetailRepoV2.delete(elementDetailV2);
                        }
                    }
                    featureElementRepoV2.delete(featureElementV2);
                }
            }
            envFeatureRepoV2.delete(envFeatureV2);
        } else {
            log.error("deleteEnvFeature() --> feature id not present");
        }
    }

    public void deleteFeatureElementV2(String elementId) {
        Optional<FeatureElementV2> featureElementV2Optional = featureElementRepoV2.findById(elementId);
//        log.debug("deleteFeatureElement() --> featureElementV2Optional --> {}", featureElementV2Optional);
        if (featureElementV2Optional.isPresent()) {
            FeatureElementV2 featureElementV2 = featureElementV2Optional.get();
//            log.debug("deleteFeatureElement() --> featureElementV2 --> {}", featureElementV2);
            if (featureElementV2 != null) {
                List<ElementDetailV2> byElementId = elementDetailRepoV2
                        .findByElementId(featureElementV2.getElementId());
                for (ElementDetailV2 elementDetailV2 : byElementId) {
//                    log.debug("deleteFeatureElement() --> elementDetailV2 --> {}", elementDetailV2);
                    if (elementDetailV2 != null) {
                        List<CHElementDetailV2> byElementDtlId = chElemDetailRepoV2
                                .findByElementDtlId(elementDetailV2.getElementDtlId());
                        for (CHElementDetailV2 chElementDetailV2 : byElementDtlId) {
//                            log.debug("deleteFeatureElement() --> chElementDetailV2 --> {}", chElementDetailV2);
                            if (chElementDetailV2 != null) {
//                                log.debug("deleteFeatureElement() --> chElementDtlId --> {}",
//                                        chElementDetailV2.getChElementDtlId());
                                chElemDetailRepoV2.delete(chElementDetailV2);
                                userConfigRepoV2.deleteUserByChElmDetailId(chElementDetailV2.getChElementDtlId());
                            }
                        }
                        elementDetailRepoV2.delete(elementDetailV2);
                    }
                }
                featureElementRepoV2.delete(featureElementV2);
            }

        } else {
            log.error("deleteFeatureElement() --> element id not present");
        }
    }

    public void deleteFeatureElementDetailsV2(String elementDtlId) {
        Optional<ElementDetailV2> elementDetailV2Optional = elementDetailRepoV2.findById(elementDtlId);
//        log.debug("deleteFeatureElementDetails() --> elementDetailV2Optional --> {}", elementDetailV2Optional);
        if (elementDetailV2Optional.isPresent()) {
            ElementDetailV2 elementDetailV2 = elementDetailV2Optional.get();
//            log.debug("deleteFeatureElementDetails() --> elementDetailV2 --> {}", elementDetailV2);
            if (elementDetailV2 != null) {
                List<CHElementDetailV2> byElementDtlId = chElemDetailRepoV2
                        .findByElementDtlId(elementDetailV2.getElementDtlId());
                for (CHElementDetailV2 chElementDetailV2 : byElementDtlId) {
//                    log.debug("deleteFeatureElementDetails() --> chElementDetailV2 --> {}", chElementDetailV2);
                    if (chElementDetailV2 != null) {
//                        log.debug("deleteFeatureElementDetails() --> chElementDtlId --> {}",
//                                chElementDetailV2.getChElementDtlId());
                        chElemDetailRepoV2.delete(chElementDetailV2);
                        userConfigRepoV2.deleteUserByChElmDetailId(chElementDetailV2.getChElementDtlId());
                    }
                }
                elementDetailRepoV2.delete(elementDetailV2);
            }
        } else {
//            log.debug("deleteFeatureElementDetails() --> element details id not present");
        }
    }

    public void deleteFeatureChildElementDetailsV2(String chElementDtlId) {
        Optional<CHElementDetailV2> chElementDetailV2Optional = chElemDetailRepoV2.findById(chElementDtlId);
//        log.debug("deleteFeatureChildElementDetails() --> elementDetailV2Optional --> {}", chElementDetailV2Optional);
        if (chElementDetailV2Optional.isPresent()) {
            CHElementDetailV2 chElementDetailV2 = chElementDetailV2Optional.get();
//            log.debug("deleteFeatureChildElementDetails() --> chElementDetailV2 --> {}", chElementDetailV2);
            if (chElementDetailV2 != null) {
//                log.debug("deleteFeatureChildElementDetails() --> chElementDtlId --> {}",
//                        chElementDetailV2.getChElementDtlId());
                chElemDetailRepoV2.delete(chElementDetailV2);
                userConfigRepoV2.deleteUserByChElmDetailId(chElementDetailV2.getChElementDtlId());
            }
        } else {
            log.error("deleteFeatureChildElementDetails() --> child element details id not present");
        }
    }

    public List<FeatureElementVOV2> createFeatureElementsOnlyV2(List<FeatureElementVOV2> featureElementVOV2List) {
//        log.info("EnvironmentServ ->createFeatureElementsOnly()");

        List<FeatureElementV2> featureElementV2List = new ArrayList<>();
        for (FeatureElementVOV2 featureElementVOV2 : featureElementVOV2List) {
//            log.debug("envFeatureVOV2 --> {}", featureElementVOV2.toString());
            FeatureElementV2 featureElementV2 = modelMapper.map(featureElementVOV2, FeatureElementV2.class);

            if (StringUtils.isBlank(featureElementV2.getElementId())) {
                featureElementV2.setElementId("ELE_" + idRepo.getNextSeqVal());
            }
//            log.debug("featureElementV2 --> {}", featureElementV2.toString());
            featureElementV2List.add(featureElementV2);
        }

        List<FeatureElementV2> featureElementV2ResponseList = featureElementRepoV2
                .saveAllAndFlush(featureElementV2List);

        return featureElementV2ResponseList.stream()
                .map(featureElementV2 -> modelMapper.map(featureElementV2, FeatureElementVOV2.class))
                .collect(Collectors.toList());
    }

    public FeatureElementVOV2 updateFeatureElementsOnlyV2(FeatureElementVOV2 featureElementVOV2, String elementId) {
//        log.info("EnvironmentServ ->updateFeatureElementsOnly()");
//
//        log.debug("envFeatureVOV2 --> {}", featureElementVOV2.toString());
        FeatureElementV2 featureElementV2 = modelMapper.map(featureElementVOV2, FeatureElementV2.class);

        if (StringUtils.isBlank(featureElementV2.getElementId())) {
            featureElementV2.setElementId(elementId);
        }
//        log.debug("featureElementV2 --> {}", featureElementV2.toString());
        FeatureElementV2 featureElementV2Res = featureElementRepoV2.saveAndFlush(featureElementV2);
        return modelMapper.map(featureElementV2Res, FeatureElementVOV2.class);
    }

    public List<ElementDetailVOV2> createFeatureElementDetailsOnlyV2(List<ElementDetailVOV2> elementDetailVOV2List) {
//        log.info("EnvironmentServ ->createFeatureElementDetailsOnly()");

        List<ElementDetailV2> elementDetailV2List = new ArrayList<>();
        for (ElementDetailVOV2 elementDetailVOV2 : elementDetailVOV2List) {
//            log.debug("elementDetailVOV2 --> {}", elementDetailVOV2.toString());
            ElementDetailV2 elementDetailV2 = modelMapper.map(elementDetailVOV2, ElementDetailV2.class);

            if (StringUtils.isBlank(elementDetailV2.getElementDtlId())) {
                elementDetailV2.setElementDtlId("ELE_DTL_" + idRepo.getNextSeqVal());
            }
//            log.debug("elementDetailV2 --> {}", elementDetailV2.toString());
            // elementDetailRepoV2.saveAndFlush(elementDetailV2);
            elementDetailV2List.add(elementDetailV2);
        }
        List<ElementDetailV2> elementDetailV2ListRes = elementDetailRepoV2.saveAllAndFlush(elementDetailV2List);

        return elementDetailV2ListRes.stream()
                .map(elementDetailV2 -> modelMapper.map(elementDetailV2, ElementDetailVOV2.class))
                .collect(Collectors.toList());
    }

    public ElementDetailVOV2 updateFeatureElementDetailsOnlyV2(ElementDetailVOV2 elementDetailVOV2,
            String elementDtlId) {
//        log.info("EnvironmentServ ->updateFeatureElementDetailsOnly()");

//        log.debug("elementDetailVOV2 --> {}", elementDetailVOV2.toString());
        ElementDetailV2 elementDetailV2 = modelMapper.map(elementDetailVOV2, ElementDetailV2.class);

        if (StringUtils.isBlank(elementDetailV2.getElementDtlId())) {
            elementDetailV2.setElementDtlId(elementDtlId);
        }
//        log.debug("elementDetailV2 --> {}", elementDetailV2.toString());
        ElementDetailV2 elementDetailV2Res = elementDetailRepoV2.saveAndFlush(elementDetailV2);
        return modelMapper.map(elementDetailV2Res, ElementDetailVOV2.class);
    }

    public List<CHElementDetailVOV2> createFeatureChildElementDetailsOnlyV2(
            List<CHElementDetailVOV2> chElementDetailVOV2List) {
//        log.info("EnvironmentServ ->createFeatureChildElementDetailsOnly()");

        List<CHElementDetailV2> chElementDetailV2List = new ArrayList<>();
        for (CHElementDetailVOV2 chElementDetailVOV2 : chElementDetailVOV2List) {
            CHElementDetailV2 chElementDetailV2 = modelMapper.map(chElementDetailVOV2, CHElementDetailV2.class);

            if (StringUtils.isBlank(chElementDetailV2.getChElementDtlId())) {
                chElementDetailV2.setChElementDtlId("ELE_DTL_CH_" + idRepo.getNextSeqVal());
            }
//            log.debug("chElementDetailV2 --> {}", chElementDetailV2.toString());
            chElementDetailV2List.add(chElementDetailV2);
        }

        List<CHElementDetailV2> chElementDetailV2ListRes = chElemDetailRepoV2.saveAllAndFlush(chElementDetailV2List);

        return chElementDetailV2ListRes.stream()
                .map(chElementDetailV2 -> modelMapper.map(chElementDetailV2, CHElementDetailVOV2.class))
                .collect(Collectors.toList());
    }

    public CHElementDetailVOV2 updateFeatureChildElementDetailsOnlyV2(CHElementDetailVOV2 chElementDetailVOV2,
            String chElementDtlId) {
//        log.info("EnvironmentServ ->updateFeatureChildElementDetailsOnly()");

        CHElementDetailV2 chElementDetailV2 = modelMapper.map(chElementDetailVOV2, CHElementDetailV2.class);

        if (StringUtils.isBlank(chElementDetailV2.getChElementDtlId())) {
            chElementDetailV2.setChElementDtlId(chElementDtlId);
        }
//        log.debug("chElementDetailV2 --> {}", chElementDetailV2.toString());
        CHElementDetailV2 chElementDetailV2Res = chElemDetailRepoV2.saveAndFlush(chElementDetailV2);
        return modelMapper.map(chElementDetailV2Res, CHElementDetailVOV2.class);
    }

    public List<EnvironmentVOV2> getAllEnvironmentsV2() {
//        log.info("EnvironmentServ ->getAllEnvironmentsV2()");
        List<EnvironmentVOV2> envs = new ArrayList<EnvironmentVOV2>();
        envRepositoryV2.findAll().forEach(env -> {
            envs.add(modelMapper.map(env, EnvironmentVOV2.class));
        });
        return envs;
    }

    public List<EnvironmentVOV3> getAllEnvironmentsV3() {
//        log.info("EnvironmentServ ->getAllEnvironmentsV3()");
        List<EnvironmentVOV3> envs = new ArrayList<>();
        envRepositoryV3.findAll().forEach(env -> {
            envs.add(modelMapper.map(env, EnvironmentVOV3.class));
        });
        return envs;
    }

    public List<TypeCompVOV2> getAllTypeCompV2() {
//        log.info("EnvironmentServ -> getAllTypeCompV2()");
        return typeCompRepoV2.findAll().stream()
                .map(typeCompV2 -> modelMapper.map(typeCompV2, TypeCompVOV2.class))
                .toList();
    }

    public EnvironmentVOV2 getEnvironmentByEnvIdV2(String envId) {
//        log.info("EnvironmentServ ->getEnvironmentV2ByEnvId()");
        Optional<EnvironmentV2> environmentV2 = envRepositoryV2.findById(envId);
        EnvironmentVOV2 environmentVOV2 = null;
        if (environmentV2.isPresent())
            environmentVOV2 = modelMapper.map(environmentV2.get(), EnvironmentVOV2.class);
        else
            throw new DashboardConfigException(
                    new DashboardConfigExceptionMessage("404", "env not found with envID " + envId));
        return environmentVOV2;
    }

    public EnvironmentVOV2 retriveEnvironmentV2ByEnvCode(String envCode) {
//        log.info("EnvironmentServ -> retriveEnvironmentV2ByEnvCode()");
        Optional<EnvironmentV2> environmentV2 = envRepositoryV2.findByEnvCode(envCode);
        EnvironmentVOV2 environmentVOV2 = null;
        if (environmentV2.isPresent())
            environmentVOV2 = modelMapper.map(environmentV2.get(), EnvironmentVOV2.class);
        else
            throw new DashboardConfigException(
                    new DashboardConfigExceptionMessage("404", "env not found with envCode " + envCode));
        return environmentVOV2;
    }

    public EnvironmentVOV3 getEnvironmentByEnvIdV3(String envId) {
//        log.info("EnvironmentServ ->getEnvironmentByEnvIdV3()");
        Optional<EnvironmentV3> environmentV3 = envRepositoryV3.findById(envId);
        EnvironmentVOV3 environmentVOV3 = null;
        if (environmentV3.isPresent())
            environmentVOV3 = modelMapper.map(environmentV3.get(), EnvironmentVOV3.class);
        else
            throw new DashboardConfigException(
                    new DashboardConfigExceptionMessage("404", "env not found with envId " + envId));
        return environmentVOV3;
    }

    public EnvironmentVOV3 retriveEnvironmentV3ByEnvCode(String envCode) {
//        log.info("EnvironmentServ -> retriveEnvironmentV3ByEnvCode()");
        Optional<EnvironmentV3> optionalEnvironmentV3 = envRepositoryV3.findByEnvCode(envCode);
        EnvironmentVOV3 environmentVOV3 = null;
        if (optionalEnvironmentV3.isPresent())
            environmentVOV3 = modelMapper.map(optionalEnvironmentV3.get(), EnvironmentVOV3.class);
        else
            throw new DashboardConfigException(
                    new DashboardConfigExceptionMessage("404", "env not found with envCode " + envCode));
        return environmentVOV3;
    }
    public void deleteEnvironmentByCode(String envCode) {
//        log.info("EnvironmentServ ->deleteEnvironmentByCode() - " + envCode);
        Environment env = envRepository.findByEnvCode(envCode);
        envRepository.delete(env);

    }

    // Environment creation activity
    public List<PlanVOV2> getAllActivePlans(String status) {
//        log.info("EnvironmentServ ->getAllActivePlans()");
        List<PlanVOV2> planVOV2List = new ArrayList<>();
        planRepoV2.findByHostPlanStatus(status).forEach(hostPlan -> {
            planVOV2List.add(modelMapper.map(hostPlan, PlanVOV2.class));
        });

        return planVOV2List;
    }

    public Map<String, List<CompVOV2>> getAllActiveComp(long planId, String status) {
//        log.info("EnvironmentServ ->getAllActiveComp()");
        return planCompRepoV2.findByHostPlan_hostPlanid(planId).stream()
                .filter(hp -> status.equalsIgnoreCase(hp.getEnvBBComp().getCompStatus()))
                .filter(hp -> hp.getHostPlanCompStatus() == null
                        || "Active".equalsIgnoreCase(hp.getHostPlanCompStatus()))
                .collect(Collectors.groupingBy(
                        hp -> hp.getPlanCompGroup().getCompGroupTitle(),
                        Collectors.mapping(this::mapEnvBBCompForAllActiveComp, Collectors.toList())));
    }

    private CompVOV2 mapEnvBBCompForAllActiveComp(HostPlanComp hostPlanComp) {
        CompVOV2 compVOV2 = modelMapper.map(hostPlanComp.getEnvBBComp(), CompVOV2.class);
        compVOV2.setSelectionType(hostPlanComp.getHostPlanCompType());
        compVOV2.setBaseDayPrice(hostPlanComp.getHostBaseMNPr());
        return compVOV2;
    }

    public List<CompSpecVOV2> getAllActiveCompSpec(String compId, String status) {
//        log.info("EnvironmentServ ->getAllActiveCompSpec()");
        List<CompSpecVOV2> compSpecVOList = new ArrayList<>();
        List<BBCompSpec> compSpecList = compRepoV2.findById(compId).get().getBbCompSpec().stream()
                .filter(spec -> spec.getCompstatus().equalsIgnoreCase(status)).collect(Collectors.toList());
        compSpecList.forEach(spec -> {
            compSpecVOList.add(modelMapper.map(spec, CompSpecVOV2.class));
        });

        return compSpecVOList;
    }

    public EnvironmentVOV3 updateEnvironmentV3(String envId, EnvironmentVOV3 environmentV0V3) {
//        log.info("EnvironmentServ -> updateEnvironmentV3");
        EnvironmentV3 environmentV3 = envRepositoryV3.findById(envId)
                .orElseThrow(() -> new RuntimeException(" We cannot find the Environment by thins " + envId + " id"));
        environmentV3.setEnvName(environmentV0V3.getEnvName());
        environmentV3.setEnvDesc(environmentV0V3.getEnvDesc());
        environmentV3.setEnvEmail(environmentV0V3.getEnvEmail());
        environmentV3.setEnvRepo(environmentV0V3.getEnvRepo());

        environmentV3.setEnvIp(environmentV0V3.getEnvIp());
        environmentV3.setEnvCustName(environmentV0V3.getEnvCustName());
        environmentV3.setEnvCustId(environmentV0V3.getEnvCustId());
        environmentV3.setEnvStatus(environmentV0V3.getEnvStatus());
        environmentV3.setEnvFapId(environmentV0V3.getEnvFapId());
        environmentV3.setEnvFatNo(environmentV0V3.getEnvFatNo());
        environmentV3.setEnvTypeId(environmentV0V3.getEnvTypeId());
        environmentV3.setEnvTenantId(environmentV0V3.getEnvTenantId());
        environmentV3.setEnvCode(environmentV0V3.getEnvCode());
        environmentV3.setEnvDomain(environmentV0V3.getEnvDomain());
        EnvironmentV3 env = envRepositoryV3.save(environmentV3);
        return modelMapper.map(env, EnvironmentVOV3.class);
    }

    public EnvFeatureVOV3 createFeatureV3(EnvFeatureVOV3 vo) {

        EnvironmentV3 environment = envRepositoryV3.findById(vo.getEnvId())
                .orElseThrow(() -> new DashboardConfigException(
                        new DashboardConfigExceptionMessage(
                                "404",
                                "Environment not found")));

        EnvFeatureV3 feature = new EnvFeatureV3();
        feature.setFeaName(vo.getFeaName());
        feature.setFeaType(vo.getFeaType());
        feature.setFeaDesc(vo.getFeaDesc());
        feature.setFeaSeq(vo.getFeaSeq());
        feature.setAssigned(vo.isAssigned());

        feature.setEnv(environment);
        environment.getEnvFeatures().add(feature);
        EnvFeatureV3 saved = envFeatureRepoV3.save(feature);

        return modelMapper.map(saved, EnvFeatureVOV3.class);
    }

    public void updateEnvFeatureV3(String featureId, EnvFeatureVOV3 vo) {

        EnvFeatureV3 feature = envFeatureRepoV3.findById(featureId)
                .orElseThrow(() -> new DashboardConfigException(
                        new DashboardConfigExceptionMessage(
                                "404",
                                "This Environment Feature is not Found!")));

        if (vo.getEnvId() != null && !feature.getEnv().getEnvId().equals(vo.getEnvId())) {
            EnvironmentV3 environment2 = envRepositoryV3.findById(feature.getEnv().getEnvId())
                    .orElseThrow(() -> new DashboardConfigException(
                            new DashboardConfigExceptionMessage(
                                    "404",
                                    "Environment not found")));
            environment2.getEnvFeatures().remove(feature);
            EnvironmentV3 environment = envRepositoryV3.findById(vo.getEnvId())
                    .orElseThrow(() -> new DashboardConfigException(
                            new DashboardConfigExceptionMessage(
                                    "404",
                                    "Environment not found")));

            environment.getEnvFeatures().add(feature);
            feature.setEnv(environment);

        }

        feature.setFeaName(vo.getFeaName());
        feature.setFeaType(vo.getFeaType());
        feature.setFeaDesc(vo.getFeaDesc());
        feature.setFeaSeq(vo.getFeaSeq());
        feature.setAssigned(vo.isAssigned());

        modelMapper.map(envFeatureRepoV3.save(feature), EnvFeatureVOV3.class);
    }

    public void deleteEnvFeatureV3(String featureId) {
        envFeatureRepoV3.deleteById(featureId);
    }

    public FeatureElementVOV3 createFeatureElementV3(FeatureElementVOV3 vo) {

        EnvFeatureV3 feature = envFeatureRepoV3.findById(vo.getFeatureId())
                .orElseThrow(() -> new DashboardConfigException(
                        new DashboardConfigExceptionMessage(
                                "404",
                                "Feature not found")));

        FeatureElementV3 element = new FeatureElementV3();

        element.setElementName(vo.getElementName());
        element.setElementType(vo.getElementType());
        element.setElementDesc(vo.getElementDesc());
        element.setElementPage(vo.getElementPage());
        element.setFeaSeq(vo.getFeaSeq());
        element.setAssigned(true);

        element.setFeature(feature);
        feature.getFeatureElements().add(element);

        FeatureElementV3 saved = featureElementRepoV3.save(element);

        return modelMapper.map(saved, FeatureElementVOV3.class);
    }

    public void updateFeatureElementV3(String elementId, FeatureElementVOV3 vo) {

        FeatureElementV3 element = featureElementRepoV3.findById(elementId)
                .orElseThrow(() -> new DashboardConfigException(
                        new DashboardConfigExceptionMessage(
                                "404",
                                "This Environment Feature Element is not Found!")));
        element.setElementName(vo.getElementName());
        element.setElementType(vo.getElementType());
        element.setElementDesc(vo.getElementDesc());
        element.setElementPage(vo.getElementPage());
        element.setFeaSeq(vo.getFeaSeq());

        modelMapper.map(featureElementRepoV3.save(element), FeatureElementVOV3.class);
    }

    public void deleteFeatureElementV3(String elementId) {
        featureElementRepoV3.deleteById(elementId);
    }

    public ElementDetailVOV3 createFeatureElementDetailV3(ElementDetailVOV3 vo) {

        FeatureElementV3 featureElement = featureElementRepoV3
                .findById(vo.getElementId())
                .orElseThrow(() -> new DashboardConfigException(
                        new DashboardConfigExceptionMessage(
                                "404",
                                "Feature Element not found")));

        ElementDetailV3 detail = new ElementDetailV3();

        detail.setElementDtlName(vo.getElementDtlName());
        detail.setElementDtlType(vo.getElementDtlType());
        detail.setElementDtlDesc(vo.getElementDtlDesc());
        detail.setElementDtlHome(vo.getElementDtlHome());
        detail.setElementDtlURL(vo.getElementDtlURL());
        detail.setElementDtlFatNo(vo.getElementDtlFatNo());
        detail.setElementDtlSeq(vo.getElementDtlSeq());
        detail.setAssigned(true);

        detail.setFeaElement(featureElement);
        featureElement.getElementDetails().add(detail);

        ElementDetailV3 saved = elementDetailRepoV3.save(detail);

        return modelMapper.map(saved, ElementDetailVOV3.class);
    }

    public void updateEnvElementDetailV3(String elementId, ElementDetailVOV3 vo) {

        ElementDetailV3 detail = elementDetailRepoV3.findById(elementId)
                .orElseThrow(() -> new DashboardConfigException(
                        new DashboardConfigExceptionMessage(
                                "404",
                                "This Environment Feature Element Detail is not Found!")));
        detail.setElementDtlName(vo.getElementDtlName());
        detail.setElementDtlType(vo.getElementDtlType());
        detail.setElementDtlDesc(vo.getElementDtlDesc());
        detail.setElementDtlHome(vo.getElementDtlHome());
        detail.setElementDtlURL(vo.getElementDtlURL());
        detail.setElementDtlFatNo(vo.getElementDtlFatNo());
        detail.setElementDtlSeq(vo.getElementDtlSeq());
        detail.setAssigned(vo.isAssigned());

    }

    public void deleteEnvElementDetailV3(String ementId) {
        elementDetailRepoV3.deleteById(ementId);
    }

    public CHElementDetailVOV3 createCHElementDetailV3(CHElementDetailVOV3 vo) {

        ElementDetailV3 parent = elementDetailRepoV3.findById(vo.getElementId()).orElseThrow(
                () -> new DashboardConfigException(
                        new DashboardConfigExceptionMessage(
                                "404",
                                "Element Detail not found")));


        if (parent == null) {
            throw new RuntimeException("Element not found with id: " + vo.getElementId());
        }

        CHElementDetailV3 child = new CHElementDetailV3();
        if (vo.getChElementDtlId() != null && !vo.getChElementDtlId().trim().isEmpty()) {
            child.setChElementDtlId(vo.getChElementDtlId());
        } else {

            long randomValue = SECURE_RANDOM.nextLong(10_000);
            child.setChElementDtlId(
                    vo.getElementId() + "_DTL_CH_" + randomValue
            );
        }
        child.setElementDtlName(vo.getElementDtlName());
        child.setElementDtlType(vo.getElementDtlType());
        child.setElementDtlDesc(vo.getElementDtlDesc());
        child.setElementDtlHome(vo.getElementDtlHome());
        child.setElementDtlURL(vo.getElementDtlURL());
        child.setElementDtlFatNo(vo.getElementDtlFatNo());
        child.setDemoUrl(vo.getElementDtlURL());
        child.setElementDtlSeq(vo.getElementDtlSeq());
        child.setAssigned(vo.isAssigned());


        // Set parent reference (replace method name with yours)
        child.setPrElemDtl(parent);

        // Update parent's collection
        parent.getChildElementDtls().add(child);

        // Save only once
        CHElementDetailV3 savedChild = chElementDetailRepoV3.save(child);

// ✅ Auto-assign access to all active users in this environment
        try {
            // Traverse: CHElementDetailV3 → ElementDetailV3 → FeatureElementV3 → EnvFeatureV3 → EnvironmentV3 → envId
            String envId = parent.getFeaElement().getFeature().getEnv().getEnvId();
            List<UserV2> activeUsers = userRepoV2.findByEnvIdAndUserStatus(envId, "active");
            List<UserConfigV2> accessRecords = new ArrayList<>();
            for (UserV2 user : activeUsers) {
                boolean alreadyAssigned = userConfigRepoV2
                        .findByUserIdAndEnvIdAndChElmDetailIdIsNotNull(user.getUserId(), envId)
                        .stream()
                        .anyMatch(u -> u.getChElmDetailId().equals(savedChild.getChElementDtlId()));
                if (!alreadyAssigned) {
                    accessRecords.add(UserConfigV2.builder()
                            .userAccessId("USR_ACC_" + idRepo.getNextSeqVal())
                            .userId(user.getUserId())
                            .envId(envId)
                            .chElmDetailId(savedChild.getChElementDtlId())
                            .build());
                }
            }
            if (!accessRecords.isEmpty()) {
                userConfigRepoV2.saveAllAndFlush(accessRecords);
//                log.info("Auto-assigned {} users access to new child element: {}", accessRecords.size(), savedChild.getChElementDtlId());
            }
        } catch (Exception e) {
            log.error("Auto-assign user access failed for new child element: {} : {}" , Utils.sanitizeForLog(savedChild.getChElementDtlId()), Utils.sanitizeForLog(e));
        }

        return modelMapper.map(savedChild, CHElementDetailVOV3.class);


    }

    public void updateCHElementDetailV3(String chelementId, CHElementDetailVOV3 vo) {

        CHElementDetailV3 child = chElementDetailRepoV3.findById(chelementId)
                .orElseThrow(() -> new DashboardConfigException(
                        new DashboardConfigExceptionMessage(
                                "404",
                                "Child Element Detail not found")));

        child.setElementDtlName(vo.getElementDtlName());
        child.setElementDtlType(vo.getElementDtlType());
        child.setElementDtlDesc(vo.getElementDtlDesc());
        child.setElementDtlHome(vo.getElementDtlHome());
        child.setElementDtlURL(vo.getElementDtlURL());
        child.setElementDtlFatNo(vo.getElementDtlFatNo());
        child.setDemoUrl(vo.getElementDtlURL());
        child.setElementDtlSeq(vo.getElementDtlSeq());
        child.setAssigned(vo.isAssigned());
        CHElementDetailV3 savedChild = chElementDetailRepoV3.save(child);

    }

    public void deleteCHElementDetailV3(String chelementId) {
        chElementDetailRepoV3.deleteById(chelementId);
    }

    // =========================================================================
    // Plan creation for environment creation
    // =========================================================================

    public List<PlanCreateVOV2> getAllPlans() {
        return planRepoV2.findAll().stream().map(this::mapHostPlanToVO).collect(Collectors.toList());
    }

    public List<PlanCompGroupVOV2> getAllPlanCompGroups() {
        return planCompGroupRepo.findAll().stream()
                .map(group -> modelMapper.map(group, PlanCompGroupVOV2.class)).collect(Collectors.toList());
    }

    public PlanCreateVOV2 createPlan(PlanCreateVOV2 vo) {
        HostPlan hostPlan = new HostPlan();
        hostPlan.setHostPlanName(vo.getHostPlanName());
        hostPlan.setHostPlanDesc(vo.getHostPlanDesc());
        hostPlan.setHostPlanCatagory(vo.getHostPlanCatagory());
        hostPlan.setHostPlanStatus(StringUtils.isBlank(vo.getHostPlanStatus()) ? "Active" : vo.getHostPlanStatus());
        hostPlan.setHostPlanSpotlight(vo.getHostPlanSpotlight());
        hostPlan.setHostPlanBasePr(vo.getHostPlanBasePr());
        hostPlan.setHostMaxPod(vo.getHostMaxPod());
        hostPlan.setHostMaxBandw(vo.getHostMaxBandw());
        hostPlan.setHostMaxAction(vo.getHostMaxAction());
        hostPlan.setHostMinDuration(vo.getHostMinDuration());
        hostPlan.setHostMaxPCT(vo.getHostMaxPCT());
        hostPlan.setHostNode(vo.getHostNode());
        hostPlan.setHostCPU(vo.getHostCPU());
        hostPlan.setHostMem(vo.getHostMem());
        hostPlan.setHostStorage(vo.getHostStorage());
        hostPlan.setActive(vo.isActive());
        hostPlan.setItemSeq(vo.getItemSeq());
        hostPlan.setHostDefaultDct(vo.getHostDefaultDct());
        hostPlan.setHostPlanDtlPageLink(vo.getHostPlanDtlPageLink());
        if (StringUtils.isNotBlank(vo.getCountryId())) {
            NnpCountry country = nnpCountryRepo.findById(vo.getCountryId())
                    .orElseThrow(() -> new DashboardConfigException(
                            new DashboardConfigExceptionMessage("404", "Country not found with id " + vo.getCountryId())));
            hostPlan.setNnpCountry(country);
        }
        HostPlan saved = planRepoV2.saveAndFlush(hostPlan);
        if (vo.getPlanComps() != null) {
            vo.getPlanComps().forEach(compVo -> savePlanComp(saved, compVo));
        }
        return mapHostPlanToVO(saved);
    }

    public PlanCreateVOV2 updatePlan(long planId, PlanCreateVOV2 vo) {
        HostPlan hostPlan = planRepoV2.findById(planId)
                .orElseThrow(() -> new DashboardConfigException(
                        new DashboardConfigExceptionMessage("404", "Plan not found with id " + planId)));
        hostPlan.setHostPlanName(vo.getHostPlanName());
        hostPlan.setHostPlanDesc(vo.getHostPlanDesc());
        hostPlan.setHostPlanCatagory(vo.getHostPlanCatagory());
        if (StringUtils.isNotBlank(vo.getHostPlanStatus()))
            hostPlan.setHostPlanStatus(vo.getHostPlanStatus());
        hostPlan.setHostPlanSpotlight(vo.getHostPlanSpotlight());
        hostPlan.setHostPlanBasePr(vo.getHostPlanBasePr());
        hostPlan.setHostMaxPod(vo.getHostMaxPod());
        hostPlan.setHostMaxBandw(vo.getHostMaxBandw());
        hostPlan.setHostMaxAction(vo.getHostMaxAction());
        hostPlan.setHostMinDuration(vo.getHostMinDuration());
        hostPlan.setHostMaxPCT(vo.getHostMaxPCT());
        hostPlan.setHostNode(vo.getHostNode());
        hostPlan.setHostCPU(vo.getHostCPU());
        hostPlan.setHostMem(vo.getHostMem());
        hostPlan.setHostStorage(vo.getHostStorage());
        hostPlan.setActive(vo.isActive());
        hostPlan.setItemSeq(vo.getItemSeq());
        hostPlan.setHostDefaultDct(vo.getHostDefaultDct());
        hostPlan.setHostPlanDtlPageLink(vo.getHostPlanDtlPageLink());
        if (StringUtils.isNotBlank(vo.getCountryId())) {
            NnpCountry country = nnpCountryRepo.findById(vo.getCountryId())
                    .orElseThrow(() -> new DashboardConfigException(
                            new DashboardConfigExceptionMessage("404", "Country not found with id " + vo.getCountryId())));
            hostPlan.setNnpCountry(country);
        }
        return mapHostPlanToVO(planRepoV2.save(hostPlan));
    }

    public void deletePlan(long planId) {
        HostPlan hostPlan = planRepoV2.findById(planId)
                .orElseThrow(() -> new DashboardConfigException(
                        new DashboardConfigExceptionMessage("404", "Plan not found with id " + planId)));
        hostPlan.setHostPlanStatus("Inactive");
        hostPlan.setActive(false);
        planRepoV2.save(hostPlan);
    }

    public PlanCompVOV2 createPlanComp(long planId, PlanCompVOV2 vo) {
        HostPlan hostPlan = planRepoV2.findById(planId)
                .orElseThrow(() -> new DashboardConfigException(
                        new DashboardConfigExceptionMessage("404", "Plan not found with id " + planId)));
        HostPlanComp existing = planCompRepoV2.findByEnvBBComp_compIdAndHostPlan_hostPlanid(vo.getCompId(), planId);
        if (existing != null && (existing.getHostPlanCompStatus() == null
                || "Active".equalsIgnoreCase(existing.getHostPlanCompStatus())))
            throw new DashboardConfigException(new DashboardConfigExceptionMessage("409",
                    "Component " + vo.getCompId() + " is already mapped to the plan"));
        return savePlanComp(hostPlan, vo);
    }

    public PlanCompVOV2 updatePlanComp(long hostRegPlanId, PlanCompVOV2 vo) {
        HostPlanComp planComp = planCompRepoV2.findById(hostRegPlanId)
                .orElseThrow(() -> new DashboardConfigException(
                        new DashboardConfigExceptionMessage("404", "Plan component mapping not found with id " + hostRegPlanId)));
        if (StringUtils.isNotBlank(vo.getCompId())) {
            BBComponent comp = compRepoV2.findById(vo.getCompId())
                    .orElseThrow(() -> new DashboardConfigException(
                            new DashboardConfigExceptionMessage("404", "Component not found with id " + vo.getCompId())));
            planComp.setEnvBBComp(comp);
        }
        if (vo.getCompGroupId() != null) {
            NNPPlanCompGroup group = planCompGroupRepo.findById(vo.getCompGroupId())
                    .orElseThrow(() -> new DashboardConfigException(
                            new DashboardConfigExceptionMessage("404", "Component group not found with id " + vo.getCompGroupId())));
            planComp.setPlanCompGroup(group);
        }
        planComp.setHostPlanCompType(vo.getHostPlanCompType());
        planComp.setHostBaseMNPr(vo.getHostBaseMNPr());
        if (StringUtils.isNotBlank(vo.getHostPlanCompStatus()))
            planComp.setHostPlanCompStatus(vo.getHostPlanCompStatus());
        return mapHostPlanCompToVO(planCompRepoV2.save(planComp));
    }

    public void deletePlanComp(long hostRegPlanId) {
        HostPlanComp planComp = planCompRepoV2.findById(hostRegPlanId)
                .orElseThrow(() -> new DashboardConfigException(
                        new DashboardConfigExceptionMessage("404", "Plan component mapping not found with id " + hostRegPlanId)));
        planComp.setHostPlanCompStatus("Inactive");
        planCompRepoV2.save(planComp);
    }

    private PlanCompVOV2 savePlanComp(HostPlan hostPlan, PlanCompVOV2 vo) {
        HostPlanComp planComp = new HostPlanComp();
        BBComponent comp = compRepoV2.findById(vo.getCompId())
                .orElseThrow(() -> new DashboardConfigException(
                        new DashboardConfigExceptionMessage("404", "Component not found with id " + vo.getCompId())));
        planComp.setEnvBBComp(comp);
        planComp.setHostPlan(hostPlan);
        if (vo.getCompGroupId() != null) {
            NNPPlanCompGroup group = planCompGroupRepo.findById(vo.getCompGroupId())
                    .orElseThrow(() -> new DashboardConfigException(
                            new DashboardConfigExceptionMessage("404", "Component group not found with id " + vo.getCompGroupId())));
            planComp.setPlanCompGroup(group);
        }
        planComp.setHostPlanCompType(vo.getHostPlanCompType());
        planComp.setHostBaseMNPr(vo.getHostBaseMNPr());
        planComp.setHostPlanCompStatus(StringUtils.isBlank(vo.getHostPlanCompStatus()) ? "Active" : vo.getHostPlanCompStatus());
        return mapHostPlanCompToVO(planCompRepoV2.saveAndFlush(planComp));
    }

    private PlanCreateVOV2 mapHostPlanToVO(HostPlan hostPlan) {
        PlanCreateVOV2 vo = new PlanCreateVOV2();
        vo.setHostPlanid(hostPlan.getHostPlanid());
        vo.setHostPlanName(hostPlan.getHostPlanName());
        vo.setHostPlanDesc(hostPlan.getHostPlanDesc());
        vo.setHostPlanCatagory(hostPlan.getHostPlanCatagory());
        vo.setHostPlanStatus(hostPlan.getHostPlanStatus());
        vo.setHostPlanSpotlight(hostPlan.getHostPlanSpotlight());
        vo.setHostPlanBasePr(hostPlan.getHostPlanBasePr());
        vo.setHostMaxPod(hostPlan.getHostMaxPod());
        vo.setHostMaxBandw(hostPlan.getHostMaxBandw());
        vo.setHostMaxAction(hostPlan.getHostMaxAction());
        vo.setHostMinDuration(hostPlan.getHostMinDuration());
        vo.setHostMaxPCT(hostPlan.getHostMaxPCT());
        vo.setHostNode(hostPlan.getHostNode());
        vo.setHostCPU(hostPlan.getHostCPU());
        vo.setHostMem(hostPlan.getHostMem());
        vo.setHostStorage(hostPlan.getHostStorage());
        vo.setActive(hostPlan.isActive());
        vo.setItemSeq(hostPlan.getItemSeq());
        vo.setHostDefaultDct(hostPlan.getHostDefaultDct());
        vo.setCountryId(hostPlan.getNnpCountry() != null ? hostPlan.getNnpCountry().getCountryId() : null);
        vo.setHostPlanDtlPageLink(hostPlan.getHostPlanDtlPageLink());
        vo.setPlanComps(planCompRepoV2.findByHostPlan_hostPlanid(hostPlan.getHostPlanid()).stream()
                .map(this::mapHostPlanCompToVO).collect(Collectors.toList()));
        return vo;
    }

    private PlanCompVOV2 mapHostPlanCompToVO(HostPlanComp planComp) {
        PlanCompVOV2 vo = new PlanCompVOV2();
        vo.setHostRegPlanId(planComp.getHostRegPlanId());
        vo.setCompId(planComp.getEnvBBComp().getCompId());
        vo.setCompName(planComp.getEnvBBComp().getCompName());
        if (planComp.getPlanCompGroup() != null) {
            vo.setCompGroupId(planComp.getPlanCompGroup().getHostPlanCompGroupId());
            vo.setCompGroupTitle(planComp.getPlanCompGroup().getCompGroupTitle());
        }
        vo.setHostPlanCompType(planComp.getHostPlanCompType());
        vo.setHostBaseMNPr(planComp.getHostBaseMNPr());
        vo.setHostPlanCompStatus(planComp.getHostPlanCompStatus());
        return vo;
    }

    // =========================================================================
    // Component creation (env_bbcomp + specs + multiple YAML templates)
    // =========================================================================

    public List<CompCreateVOV2> getAllComponents() {
        return compRepoV2.findAll().stream().map(this::mapComponentToVO).collect(Collectors.toList());
    }

    public CompCreateVOV2 createComponent(CompCreateVOV2 vo) {
        BBComponent comp = new BBComponent();
        comp.setCompId(StringUtils.isBlank(vo.getCompId()) ? "COMP_" + idRepo.getNextSeqVal() : vo.getCompId());
        if (compRepoV2.existsById(comp.getCompId()))
            throw new DashboardConfigException(
                    new DashboardConfigExceptionMessage("409", "Component already exists with id " + comp.getCompId()));
        comp.setCompName(vo.getCompName());
        comp.setCompDesc(vo.getCompDesc());
        comp.setCompStatus(StringUtils.isBlank(vo.getCompStatus()) ? "Active" : vo.getCompStatus());
        comp.setCompType(vo.getCompType());
        comp.setItemSeq(vo.getItemSeq());
        comp.setEnvPlatform(vo.getEnvPlatform());
        comp.setEnvGitPath(vo.getEnvGitPath());
        comp.setEnvGitToken(vo.getEnvGitToken());
        comp.setSgaredCompServUrl(vo.getSgaredCompServUrl());
        comp.setK8sCompName(vo.getK8sCompName());
        comp.setProxyExpose(vo.isProxyExpose());
        BBComponent saved = compRepoV2.saveAndFlush(comp);

        if (vo.getSpecs() != null) {
            vo.getSpecs().forEach(specVo -> saveCompSpec(saved, specVo));
        }
        if (vo.getTemplates() != null) {
            vo.getTemplates().forEach(tmplVo -> saveCompTemplate(saved, tmplVo));
        }
        return mapComponentToVO(saved);
    }

    public CompCreateVOV2 updateComponent(String compId, CompCreateVOV2 vo) {
        BBComponent comp = compRepoV2.findById(compId)
                .orElseThrow(() -> new DashboardConfigException(
                        new DashboardConfigExceptionMessage("404", "Component not found with id " + compId)));
        comp.setCompName(vo.getCompName());
        comp.setCompDesc(vo.getCompDesc());
        if (StringUtils.isNotBlank(vo.getCompStatus()))
            comp.setCompStatus(vo.getCompStatus());
        comp.setCompType(vo.getCompType());
        comp.setItemSeq(vo.getItemSeq());
        comp.setEnvPlatform(vo.getEnvPlatform());
        comp.setEnvGitPath(vo.getEnvGitPath());
        if (StringUtils.isNotBlank(vo.getEnvGitToken()))
            comp.setEnvGitToken(vo.getEnvGitToken());
        comp.setSgaredCompServUrl(vo.getSgaredCompServUrl());
        comp.setK8sCompName(vo.getK8sCompName());
        comp.setProxyExpose(vo.isProxyExpose());
        return mapComponentToVO(compRepoV2.save(comp));
    }

    public void deleteComponent(String compId) {
        BBComponent comp = compRepoV2.findById(compId)
                .orElseThrow(() -> new DashboardConfigException(
                        new DashboardConfigExceptionMessage("404", "Component not found with id " + compId)));
        comp.setCompStatus("Inactive");
        compRepoV2.save(comp);
    }

    public CompSpecVOV2 createCompSpec(String compId, CompSpecVOV2 vo) {
        BBComponent comp = compRepoV2.findById(compId)
                .orElseThrow(() -> new DashboardConfigException(
                        new DashboardConfigExceptionMessage("404", "Component not found with id " + compId)));
        return saveCompSpec(comp, vo);
    }

    public CompSpecVOV2 updateCompSpec(String specId, CompSpecVOV2 vo) {
        BBCompSpec spec = compSpecRepoV2.findById(specId)
                .orElseThrow(() -> new DashboardConfigException(
                        new DashboardConfigExceptionMessage("404", "Spec not found with id " + specId)));
        spec.setSpecName(vo.getSpecName());
        spec.setSpecType(vo.getSpecType());
        spec.setSpecvalidation(vo.getSpecvalidation());
        spec.setSpecdesc(vo.getSpecdesc());
        spec.setSpecvalues(vo.getSpecvalues());
        spec.setTmplSpecVarName(vo.getTmplSpecVarName());
        spec.setEditable(vo.isEditable());
        if (StringUtils.isNotBlank(vo.getCompstatus()))
            spec.setCompstatus(vo.getCompstatus());
        return modelMapper.map(compSpecRepoV2.save(spec), CompSpecVOV2.class);
    }

    public void deleteCompSpec(String specId) {
        BBCompSpec spec = compSpecRepoV2.findById(specId)
                .orElseThrow(() -> new DashboardConfigException(
                        new DashboardConfigExceptionMessage("404", "Spec not found with id " + specId)));
        spec.setCompstatus("Inactive");
        compSpecRepoV2.save(spec);
    }

    private CompSpecVOV2 saveCompSpec(BBComponent comp, CompSpecVOV2 vo) {
        BBCompSpec spec = new BBCompSpec();
        spec.setSpecid(StringUtils.isBlank(vo.getSpecid()) ? "SPEC_" + idRepo.getNextSeqVal() : vo.getSpecid());
        spec.setSpecName(vo.getSpecName());
        spec.setSpecType(vo.getSpecType());
        spec.setSpecvalidation(vo.getSpecvalidation());
        spec.setSpecdesc(vo.getSpecdesc());
        spec.setSpecvalues(vo.getSpecvalues());
        spec.setTmplSpecVarName(vo.getTmplSpecVarName());
        spec.setEditable(vo.isEditable());
        spec.setCompstatus(StringUtils.isBlank(vo.getCompstatus()) ? "Active" : vo.getCompstatus());
        spec.setBbComponent(comp);
        return modelMapper.map(compSpecRepoV2.saveAndFlush(spec), CompSpecVOV2.class);
    }

    public List<CompTemplateVOV2> getCompTemplates(String compId) {
        return bbCompTemplateRepo.findByBbComponent_compId(compId).stream()
                .map(this::mapTemplateToVO).collect(Collectors.toList());
    }

    public CompTemplateVOV2 createCompTemplate(String compId, CompTemplateVOV2 vo) {
        BBComponent comp = compRepoV2.findById(compId)
                .orElseThrow(() -> new DashboardConfigException(
                        new DashboardConfigExceptionMessage("404", "Component not found with id " + compId)));
        return saveCompTemplate(comp, vo);
    }

    public CompTemplateVOV2 updateCompTemplate(String tmplId, CompTemplateVOV2 vo) {
        BBCompTemplate tmpl = bbCompTemplateRepo.findById(tmplId)
                .orElseThrow(() -> new DashboardConfigException(
                        new DashboardConfigExceptionMessage("404", "Template not found with id " + tmplId)));
        tmpl.setTemplate(vo.getTemplate());
        if (StringUtils.isNotBlank(vo.getFileName()))
            tmpl.setFileName(vo.getFileName());
        if (StringUtils.isNotBlank(vo.getFilePath()))
            tmpl.setFilePath(vo.getFilePath());
        if (StringUtils.isNotBlank(vo.getTmplType()))
            tmpl.setTmplType(vo.getTmplType());
        if (StringUtils.isNotBlank(vo.getCreatedBy()))
            tmpl.setCreatedBy(vo.getCreatedBy());
        if (StringUtils.isNotBlank(vo.getStatus()))
            tmpl.setStatus(vo.getStatus());
        return mapTemplateToVO(bbCompTemplateRepo.save(tmpl));
    }

    public void deleteCompTemplate(String tmplId) {
        BBCompTemplate tmpl = bbCompTemplateRepo.findById(tmplId)
                .orElseThrow(() -> new DashboardConfigException(
                        new DashboardConfigExceptionMessage("404", "Template not found with id " + tmplId)));
        tmpl.setStatus("Inactive");
        bbCompTemplateRepo.save(tmpl);
    }

    private CompTemplateVOV2 saveCompTemplate(BBComponent comp, CompTemplateVOV2 vo) {
        BBCompTemplate tmpl = new BBCompTemplate();
        tmpl.setTmplId(StringUtils.isBlank(vo.getTmplId()) ? "TMPL_" + idRepo.getNextSeqVal() : vo.getTmplId());
        tmpl.setTemplate(vo.getTemplate());
        tmpl.setFileName(vo.getFileName());
        tmpl.setFilePath(vo.getFilePath());
        tmpl.setTmplType(vo.getTmplType());
        tmpl.setCreatedBy(vo.getCreatedBy());
        tmpl.setStatus(StringUtils.isBlank(vo.getStatus()) ? "Active" : vo.getStatus());
        tmpl.setBbComponent(comp);
        return mapTemplateToVO(bbCompTemplateRepo.saveAndFlush(tmpl));
    }

    private CompCreateVOV2 mapComponentToVO(BBComponent comp) {
        CompCreateVOV2 vo = new CompCreateVOV2();
        vo.setCompId(comp.getCompId());
        vo.setCompName(comp.getCompName());
        vo.setCompDesc(comp.getCompDesc());
        vo.setCompStatus(comp.getCompStatus());
        vo.setCompType(comp.getCompType());
        vo.setItemSeq(comp.getItemSeq());
        vo.setEnvPlatform(comp.getEnvPlatform());
        vo.setEnvGitPath(comp.getEnvGitPath());
        vo.setEnvGitToken(comp.getEnvGitToken());
        vo.setSgaredCompServUrl(comp.getSgaredCompServUrl());
        vo.setK8sCompName(comp.getK8sCompName());
        vo.setProxyExpose(comp.isProxyExpose());
        vo.setSpecs(comp.getBbCompSpec().stream().map(spec -> modelMapper.map(spec, CompSpecVOV2.class))
                .collect(Collectors.toList()));
        vo.setTemplates(comp.getBbCompTempl().stream().map(this::mapTemplateToVO).collect(Collectors.toList()));
        return vo;
    }

    private CompTemplateVOV2 mapTemplateToVO(BBCompTemplate tmpl) {
        CompTemplateVOV2 vo = new CompTemplateVOV2();
        vo.setTmplId(tmpl.getTmplId());
        vo.setTemplate(tmpl.getTemplate());
        vo.setFileName(tmpl.getFileName());
        vo.setFilePath(tmpl.getFilePath());
        vo.setTmplType(tmpl.getTmplType());
        vo.setCreatedBy(tmpl.getCreatedBy());
        vo.setStatus(tmpl.getStatus());
        return vo;
    }
}
