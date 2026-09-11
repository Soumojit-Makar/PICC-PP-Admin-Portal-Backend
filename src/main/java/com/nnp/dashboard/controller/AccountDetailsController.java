package com.nnp.dashboard.controller;

import com.nnp.dashboard.vo.AccountDetailsDTOV0;
import com.nnp.dashboard.service.AccountDetailService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * REST controller exposing account/environment "details" used by the billing or
 * account-administration screens of the portal.
 *
 * GET  /accounts/details?accountName=...   -> read account + environment + admin user info
 * PUT  /accounts/details/{account}/{status}-> update those details and the account status
 *
 * Delegates to {@link AccountDetailService}.
 */
@RestController
@RequestMapping("/accounts")
public class AccountDetailsController {
    private final AccountDetailService accountDetailService;
    public AccountDetailsController(AccountDetailService accountDetailsService){
        this.accountDetailService =accountDetailsService;
    }
    @GetMapping("details")
    public AccountDetailsDTOV0 getAccountDetails(@RequestParam String accountName){
        return accountDetailService.getAccountDetails(accountName);
    }
    @PutMapping("details/{account-name}/{account-status}")
    public Map<String,String> updateAccountDetails(
            @PathVariable("account-name") String accountName,
            @PathVariable("account-status") String accountStatus,
            @RequestBody @Valid AccountDetailsDTOV0 accountDetailsVO
    ){
        accountDetailService.updateAccountDetails(accountName,accountDetailsVO,accountStatus);
        return Map.of(
                "message","Update Successful"
        );
    }
}
