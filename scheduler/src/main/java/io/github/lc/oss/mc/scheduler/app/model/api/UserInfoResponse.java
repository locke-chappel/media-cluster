package io.github.lc.oss.mc.scheduler.app.model.api;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

@JsonInclude(Include.NON_EMPTY)
public class UserInfoResponse extends io.github.lc.oss.commons.identity.model.UserInfoResponse<UserInfo> {

}
