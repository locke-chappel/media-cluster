package io.github.lc.oss.mc.scheduler.app.model;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

@JsonInclude(Include.NON_EMPTY)
@JsonIgnoreProperties(ignoreUnknown = true)
public class Profile extends io.github.lc.oss.mc.api.Profile {
    public Profile() {
        super();
    }

    public Profile(io.github.lc.oss.mc.scheduler.app.entity.Profile profile, //
            String ext, //
            int sliceLength, //
            List<String> audioArgs, //
            List<String> videoArgs, //
            List<String> commonArgs) {
        super(profile, profile.getName(), ext, sliceLength, audioArgs, videoArgs, commonArgs);
    }
}
