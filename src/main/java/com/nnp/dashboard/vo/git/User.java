package com.nnp.dashboard.vo.git;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

import lombok.Getter;
import lombok.Setter;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({
        "id",
        "username",
        "name",
        "state",
        "avatar_url",
        "web_url",
        "created_at",
        "bio",
        "location",
        "public_email",
        "skype",
        "linkedin",
        "twitter",
        "website_url",
        "organization",
        "job_title",
        "pronouns",
        "bot",
        "work_information",
        "followers",
        "following",
        "local_time",
        "last_sign_in_at",
        "confirmed_at",
        "last_activity_on",
        "email",
        "theme_id",
        "color_scheme_id",
        "projects_limit",
        "current_sign_in_at",
        "identities",
        "can_create_group",
        "can_create_project",
        "two_factor_enabled",
        "external",
        "private_profile",
        "commit_email",
        "is_admin",
        "note",
        "access_level"
})
@Setter
@Getter
public class User {
    @JsonProperty("id")
    private Integer id;
    @JsonProperty("username")
    private String username;
    @JsonProperty("name")
    private String name;
    @JsonProperty("state")
    private String state;
    @JsonProperty("avatar_url")
    private String avatarUrl;
    @JsonProperty("web_url")
    private String webUrl;
    @JsonProperty("created_at")
    private String createdAt;
    @JsonProperty("bio")
    private String bio;
    @JsonProperty("location")
    private Object location;
    @JsonProperty("public_email")
    private Object publicEmail;
    @JsonProperty("skype")
    private String skype;
    @JsonProperty("linkedin")
    private String linkedin;
    @JsonProperty("twitter")
    private String twitter;
    @JsonProperty("website_url")
    private String websiteUrl;
    @JsonProperty("organization")
    private Object organization;
    @JsonProperty("job_title")
    private String jobTitle;
    @JsonProperty("pronouns")
    private Object pronouns;
    @JsonProperty("bot")
    private Boolean bot;
    @JsonProperty("work_information")
    private Object workInformation;
    @JsonProperty("followers")
    private Integer followers;
    @JsonProperty("following")
    private Integer following;
    @JsonProperty("local_time")
    private Object localTime;
    @JsonProperty("last_sign_in_at")
    private Object lastSignInAt;
    @JsonProperty("confirmed_at")
    private Object confirmedAt;
    @JsonProperty("last_activity_on")
    private Object lastActivityOn;
    @JsonProperty("email")
    private String email;
    @JsonProperty("theme_id")
    private Integer themeId;
    @JsonProperty("color_scheme_id")
    private Integer colorSchemeId;
    @JsonProperty("projects_limit")
    private Integer projectsLimit;
    @JsonProperty("current_sign_in_at")
    private Object currentSignInAt;
    @JsonProperty("identities")
    private List<Object> identities = null;
    @JsonProperty("can_create_group")
    private Boolean canCreateGroup;
    @JsonProperty("can_create_project")
    private Boolean canCreateProject;
    @JsonProperty("two_factor_enabled")
    private Boolean twoFactorEnabled;
    @JsonProperty("external")
    private Boolean external;
    @JsonProperty("private_profile")
    private Boolean privateProfile;
    @JsonProperty("commit_email")
    private String commitEmail;
    @JsonProperty("is_admin")
    private Boolean isAdmin;
    @JsonProperty("note")
    private Object note;
    @JsonProperty("access_level")
    private Integer accessLevel;
}
