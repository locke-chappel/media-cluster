package io.github.lc.oss.mc.api;

import java.util.List;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

@JsonInclude(Include.NON_EMPTY)
@JsonIgnoreProperties(ignoreUnknown = true)
public class Profile extends Entity {
    private String name;
    private String ext;
    private Integer sliceLength;
    private List<String> audioArgs;
    private List<String> videoArgs;
    private List<String> commonArgs;

    public Profile() {
        super();
    }

    public Profile(AbstractEntity entity, //
            String name, //
            String ext, //
            Integer sliceLength, //
            List<String> audioArgs, //
            List<String> videoArgs, //
            List<String> commonArgs) {
        super(entity);
        this.name = name;
        this.ext = ext;
        this.sliceLength = sliceLength;
        this.setAudioArgs(audioArgs);
        this.setVideoArgs(videoArgs);
        this.setCommonArgs(commonArgs);
    }

    public String getName() {
        return this.name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getExt() {
        return this.ext;
    }

    public void setExt(String ext) {
        this.ext = ext;
    }

    public Integer getSliceLength() {
        return this.sliceLength;
    }

    public void setSliceLength(Integer sliceLength) {
        this.sliceLength = sliceLength;
    }

    public List<String> getAudioArgs() {
        return this.audioArgs;
    }

    public void setAudioArgs(List<String> audioArgs) {
        List<String> args = audioArgs;
        if (args != null) {
            args = audioArgs.stream().filter(arg -> StringUtils.isNotBlank(arg)).collect(Collectors.toList());
        }
        this.audioArgs = args;
    }

    public List<String> getVideoArgs() {
        return this.videoArgs;
    }

    public void setVideoArgs(List<String> videoArgs) {
        List<String> args = videoArgs;
        if (args != null) {
            args = videoArgs.stream().filter(arg -> StringUtils.isNotBlank(arg)).collect(Collectors.toList());
        }
        this.videoArgs = args;
    }

    public List<String> getCommonArgs() {
        return this.commonArgs;
    }

    public void setCommonArgs(List<String> commonArgs) {
        List<String> args = commonArgs;
        if (args != null) {
            args = commonArgs.stream().filter(arg -> StringUtils.isNotBlank(arg)).collect(Collectors.toList());
        }
        this.commonArgs = args;
    }

    public boolean hasAudio() {
        if (this.getAudioArgs() == null) {
            return true;
        }
        return !this.getAudioArgs().stream().anyMatch(arg -> StringUtils.equals("-an", arg));
    }

    public boolean hasVideo() {
        if (this.getVideoArgs() == null) {
            return true;
        }
        return !this.getVideoArgs().stream().anyMatch(arg -> StringUtils.equals("-vn", arg));
    }
}
