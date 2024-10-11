package io.github.lc.oss.mc.scheduler.app.model.api;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

import io.github.lc.oss.mc.scheduler.app.model.Credentials;

import com.fasterxml.jackson.annotation.JsonProperty;

@JsonInclude(Include.NON_EMPTY)
public class LoginRequest extends Credentials implements io.github.lc.oss.commons.api.identity.LoginRequest {
    private final String applicationId;

    public LoginRequest( //
            @JsonProperty("username") String username, //
            @JsonProperty("password") String password, //
            @JsonProperty("applicationId") String applicationId) //
    {
        super(username, password);
        this.applicationId = applicationId;
    }

    @Override
    public String getApplicationId() {
        return this.applicationId;
    }
}
