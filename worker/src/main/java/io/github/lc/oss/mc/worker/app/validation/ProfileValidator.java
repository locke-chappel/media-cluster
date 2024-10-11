package io.github.lc.oss.mc.worker.app.validation;

import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import io.github.lc.oss.commons.serialization.Message;
import io.github.lc.oss.mc.api.Messages;
import io.github.lc.oss.mc.api.Profile;
import io.github.lc.oss.mc.validation.AbstractValidator;
import io.github.lc.oss.mc.validation.CommandValidator;
import io.github.lc.oss.mc.validation.Patterns;

@Component
public class ProfileValidator extends AbstractValidator<Profile> {
    @Autowired
    private CommandValidator commandValidator;

    @Override
    public Set<Message> validate(Profile instance) {
        Set<Message> messages = this.valid();
        if (this.missingValue(instance)) {
            messages.add(this.toMessage(Messages.Application.RequiredFieldMissing, this.getFieldVar("Profile")));
            return messages;
        }

        this.validateString(messages, "Profile.Ext", Patterns.FileExt, true, instance.getExt());

        Integer sliceLength = instance.getSliceLength();
        if (sliceLength != null && (sliceLength < 1 || sliceLength > 999999999)) {
            messages.add(this.toMessage(Messages.Application.InvalidField, this.getFieldVar("Profile.SliceLength")));
        }

        if (instance.getAudioArgs() != null) {
            for (String arg : instance.getAudioArgs()) {
                this.merge(messages, this.commandValidator, arg);
            }
        }

        if (instance.getVideoArgs() != null) {
            for (String arg : instance.getVideoArgs()) {
                this.merge(messages, this.commandValidator, arg);
            }
        }

        if (instance.getCommonArgs() != null) {
            for (String arg : instance.getCommonArgs()) {
                this.merge(messages, this.commandValidator, arg);
            }
        }

        return messages;
    }

}
