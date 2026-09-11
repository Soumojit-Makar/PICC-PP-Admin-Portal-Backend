package com.nnp.dashboard.vo;

public record AccountDetailsDTOV0(
        String contactName,
        String email,
        String phone,
        String address,
        String organization,
        String platformUsePurpose,
        String administrativeAccess,
        String userAccess,
        String userToken,
        String adminToken,
        String primaryUsername,
        String repoName,
        String domain
) {
}
