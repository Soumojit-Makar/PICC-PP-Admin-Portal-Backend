package com.nnp.dashboard.vo.kc;

import lombok.*;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString
public class UserUpdate {

    private String username;
    private String email;
    private String firstName;
    private String lastName;
    private String emailVerified;
    private List<String> requiredActions;
    //private List<String> groups;
    private boolean enabled = true;
    //private List<UserCredential> credentials;
    private UserAttributes attributes;
    //private UserAccess access;

}
