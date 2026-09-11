package com.nnp.dashboard.vo.rm;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class RootProject {

    private String projectId;
    private String userId;
    private String name;
    private String identifier;
    private String description;
    private String login;
    private String firstname;
    private String lastname;
    private String mail;
    private String password;

}
