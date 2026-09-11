package com.nnp.dashboard.vo.rm;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({
        "id",
        "login",
        "admin",
        "firstname",
        "lastname",
        "mail",
        "created_on",
        "last_login_on",
        "api_key",
        "status"
})
@Setter
@Getter
@ToString
public class UserResp {
    @JsonProperty("id")
    private Integer id;
    @JsonProperty("login")
    private String login;
    @JsonProperty("admin")
    private Boolean admin;
    @JsonProperty("firstname")
    private String firstname;
    @JsonProperty("lastname")
    private String lastname;
    @JsonProperty("mail")
    private String mail;
    @JsonProperty("created_on")
    private String createdOn;
    @JsonProperty("last_login_on")
    private Object lastLoginOn;
    @JsonProperty("api_key")
    private String apiKey;
    @JsonProperty("status")
    private Integer status;
}
