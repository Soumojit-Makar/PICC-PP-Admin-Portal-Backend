
package com.nnp.dashboard.vo.rm;

import java.util.Map;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({
    "login",
    "firstname",
    "lastname",
    "mail",
    "password"
})

@Getter
@Setter
@ToString
public class User__1 {

    @JsonProperty("login")
    private String login;
    @JsonProperty("firstname")
    private String firstname;
    @JsonProperty("lastname")
    private String lastname;
    @JsonProperty("mail")
    private String mail;
    @JsonProperty("password")
    private String password;
    
}
