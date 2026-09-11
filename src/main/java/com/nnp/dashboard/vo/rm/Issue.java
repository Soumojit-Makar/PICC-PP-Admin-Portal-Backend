package com.nnp.dashboard.vo.rm;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import lombok.Getter;
import lombok.Setter;

import java.util.HashMap;
import java.util.Map;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({
        "issue"
})
@Getter
@Setter
public class Issue {
    @JsonProperty("issue")
    private Issue__1 issue;
    @JsonIgnore
    private Map<String, Object> additionalProperties = new HashMap<>();
}
