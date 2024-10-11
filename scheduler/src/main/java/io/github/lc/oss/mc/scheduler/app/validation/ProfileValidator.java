package io.github.lc.oss.mc.scheduler.app.validation;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import io.github.lc.oss.commons.serialization.Message;
import io.github.lc.oss.mc.api.Messages;
import io.github.lc.oss.mc.scheduler.app.model.Profile;
import io.github.lc.oss.mc.validation.CommandValidator;
import io.github.lc.oss.mc.validation.Patterns;

@Component
public class ProfileValidator extends AbstractValidator<Profile> {
    private static class ArgValidationResponse {
        public Set<Message> messages = new HashSet<>();
        public boolean disabledAudio = false;
        public boolean disabledVideo = false;
    }

    @Autowired
    private CommandValidator commandValidator;

    @Override
    public Set<Message> validate(Profile instance) {
        Set<Message> messages = this.valid();
        if (this.missingValue(instance)) {
            messages.add(this.toMessage(Messages.Application.RequiredFieldMissing, this.getFieldVar("Profile")));
            return messages;
        }

        this.validateString(messages, "settings.profiles.profile", Patterns.Name, true, instance.getName());
        this.validateString(messages, "ffmpeg.ext", Patterns.FileExt, true, instance.getExt());

        Integer sliceLength = instance.getSliceLength();
        if (sliceLength != null && (sliceLength < 1 || sliceLength > 999999999)) {
            messages.add(
                    this.toMessage(Messages.Application.InvalidField, this.getFieldVar("ffmpeg.video.sliceLength")));
        }

        ArgValidationResponse videoArgsResponse = this.validateArgs(instance.getVideoArgs());
        ArgValidationResponse audioArgsResponse = this.validateArgs(instance.getAudioArgs());
        ArgValidationResponse commonArgsResponse = this.validateArgs(instance.getCommonArgs());

        messages.addAll(videoArgsResponse.messages);
        messages.addAll(audioArgsResponse.messages);
        messages.addAll(commonArgsResponse.messages);

        boolean disabledVideo = videoArgsResponse.disabledVideo || //
                commonArgsResponse.disabledVideo;
        boolean disabledAudio = audioArgsResponse.disabledAudio || //
                commonArgsResponse.disabledAudio;

        if (disabledAudio && disabledVideo) {
            messages.add(this.toMessage(Messages.Application.NothingToProcess));
        }

        return messages;
    }

    private ArgValidationResponse validateArgs(List<String> args) {
        ArgValidationResponse response = new ArgValidationResponse();
        if (args == null || args.isEmpty()) {
            return response;
        }

        String arg;
        String value;
        for (int i = 0; i < args.size(); i++) {
            arg = args.get(i);
            switch (arg) {
                case "-vf":
                    value = args.get(i + 1);
                    if (StringUtils.contains(value, "trim")) {
                        response.messages.add(this.toMessage(Messages.Application.ArgsIncludesTrimFilter));
                    }
                    break;
                case "-an":
                    response.disabledAudio = true;
                    break;
                case "-vn":
                    response.disabledVideo = true;
                    break;
                default:
                    // no-op
                    break;
            }

            this.merge(response.messages, this.commandValidator, arg);
        }

        return response;
    }
}
