package com.nnp.dashboard.service.imp;

import com.nnp.dashboard.exception.DashboardConfigException;
import com.nnp.dashboard.exception.DashboardConfigExceptionMessage;
import com.nnp.dashboard.model.Environment;
import com.nnp.dashboard.model.NnpAccount;
import com.nnp.dashboard.model.UserV2;
import com.nnp.dashboard.repo.EnvironmentRepo;
import com.nnp.dashboard.repo.NnpAccountRepo;
import com.nnp.dashboard.repo.UserRepoV2;
import com.nnp.dashboard.service.AccountDetailService;
import com.nnp.dashboard.vo.AccountDetailsDTOV0;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;


/**
 * Implementation of {@link AccountDetailService}.
 *
 * Fetches/updates the "account detail sheet" shown in the portal admin screen.
 * The details are spread over three entities linked by the account:
 *   - NnpAccount      : organization, status
 *   - Environment     : admin/user comm template, K8s tokens, description,
 *                       customer (envCustId/envCustName), repo &amp; domain
 *   - UserV2 (envCustId): the account master user's personal data
 *
 * On update it also transitions the account status (e.g. to Active/Inactive)
 * and rewrites the master user + environment fields with the submitted payload.
 */
@Service
@Log4j2
public class AccountDetailServiceImp implements AccountDetailService {
    private final NnpAccountRepo nnpAccountRepo;
    private final UserRepoV2 userRepoV2;
    private final EnvironmentRepo environmentRepo;
    @Autowired
    public AccountDetailServiceImp(NnpAccountRepo nnpAccountRepo,UserRepoV2 userRepoV2,EnvironmentRepo environmentRepo){
        this.nnpAccountRepo=nnpAccountRepo;
        this.userRepoV2=userRepoV2;
        this.environmentRepo=environmentRepo;
    }

    @Override
    public void updateAccountDetails(String accountName, AccountDetailsDTOV0 accountDetailsVO,String status) {
        NnpAccount nnpAccount= nnpAccountRepo
                .findByAccName(accountName)
                .orElseThrow(()->
                        new DashboardConfigException(
                                new DashboardConfigExceptionMessage(
                                        "404",
                                        "This Account Does Not Exist"
                                )
                        )
                );
        Environment env =environmentRepo.findById(nnpAccount.getEnv()).orElseThrow(()->
                new DashboardConfigException(
                        new DashboardConfigExceptionMessage(
                                "404",
                                "This Env Does Not Exist"
                        )
                )
        );
        String envMasterUserId =env.getEnvCustId();
        if(!StringUtils.hasText(envMasterUserId))
            throw new DashboardConfigException(new DashboardConfigExceptionMessage("500",
                    "There Is No Master User (EnvCustId) In Env : " + env.getEnvCode()
            ));
        UserV2 accountMasterUser = userRepoV2.findById(envMasterUserId).orElseThrow(
                ()->new DashboardConfigException(new DashboardConfigExceptionMessage(
                        "500",
                        "No User With Id : " + envMasterUserId
                ))
        );
        String[] nameArray =accountDetailsVO.contactName().split(" ",2);
        String firstName=nameArray[0];
        String lastName=nameArray[1];
        accountMasterUser.setFirstName(firstName);
        accountMasterUser.setLastName(lastName);
        accountMasterUser.setEmailId(accountDetailsVO.email());
        accountMasterUser.setContactNumber(accountDetailsVO.phone());
        accountMasterUser.setAddress(accountDetailsVO.address());
        nnpAccount.setOrganization(accountDetailsVO.organization());
        nnpAccount.setAccStatus(status);
        env.setAdminComm(accountDetailsVO.administrativeAccess());
        env.setAdminK8sNsToken(accountDetailsVO.adminToken());
        env.setUserK8sNsToken(accountDetailsVO.userToken());
        env.setUserComm(accountDetailsVO.userAccess());
        env.setEnvDesc(accountDetailsVO.platformUsePurpose());
        userRepoV2.save(accountMasterUser);
        nnpAccountRepo.save(nnpAccount);
        environmentRepo.save(env);
    }

    @Override
    public AccountDetailsDTOV0 getAccountDetails(String accountName) {
       NnpAccount nnpAccount= nnpAccountRepo
               .findByAccName(accountName)
               .orElseThrow(()->
                       new DashboardConfigException(
                               new DashboardConfigExceptionMessage(
                                       "404",
                                       "This Account Does Not Exist"
                               )
                       )
               );
       Environment env =environmentRepo.findById(nnpAccount.getEnv()).orElseThrow(()->
                new DashboardConfigException(
                        new DashboardConfigExceptionMessage(
                                "404",
                                "This Env Does Not Exist"
                        )
                )
       );
       String envMasterUserId =env.getEnvCustId();
       if(!StringUtils.hasText(envMasterUserId))
           throw new DashboardConfigException(new DashboardConfigExceptionMessage("500",
                   "There Is No Master User (EnvCustId) In Env : " + env.getEnvCode()
           ));
        UserV2 userV2 = userRepoV2.findById(envMasterUserId).orElseThrow(
                ()->new DashboardConfigException(new DashboardConfigExceptionMessage(
                        "500",
                        "No User With Id : " + envMasterUserId
                ))
        );

        return new AccountDetailsDTOV0(
                userV2.getFirstName()+" "+userV2.getLastName(),
                userV2.getEmailId(),
                userV2.getContactNumber(),
                userV2.getAddress(),
                nnpAccount.getOrganization(),
                env.getEnvDesc(),
                env.getAdminComm(),
                env.getUserComm(),
                env.getUserK8sNsToken(),
                env.getAdminK8sNsToken(),
                env.getEnvCustId(),
                env.getEnvRepo(),
                env.getEnvDomain()
        );
    }
    }
