package com.nnp.dashboard.vo.rm;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({
        "user"
})
@Setter
@Getter
@ToString
public class ResponseUser {
    @JsonProperty("user")
    public UserResp user;
}