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
        "subject",
        "notes"
})
@Getter
@Setter
public class Issue__1 {
    @JsonProperty("project_id")
    private Integer projectId;
    @JsonProperty("subject")
    private String subject;
    @JsonProperty("priority_id")
    private Integer priorityId;
    @JsonProperty("tracker_id")
    private Integer trackerId;
    @JsonProperty("status_id")
    private Integer statusId;
    @JsonProperty("description")
    private String description;
    @JsonProperty("author_id")
    private String authorId;
    @JsonProperty("assigned_to_id")
    private String assignedToId;
    @JsonProperty("category_id")
    private String categoryId;
    @JsonIgnore
    private Map<String, Object> additionalProperties = new HashMap<>();
}
