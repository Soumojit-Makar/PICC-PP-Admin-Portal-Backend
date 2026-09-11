package com.nnp.dashboard.service;

import com.nnp.dashboard.vo.AccountDetailsDTOV0;
import jakarta.validation.Valid;

public interface AccountDetailService {
    void updateAccountDetails(String accountName, @Valid AccountDetailsDTOV0 accountDetailsVO,String status);

    AccountDetailsDTOV0 getAccountDetails(String accountName);
}
