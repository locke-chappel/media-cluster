package io.github.lc.oss.mc.validation;

import java.util.Set;

import org.apache.commons.lang3.StringUtils;

import io.github.lc.oss.commons.serialization.Message;
import io.github.lc.oss.mc.api.Messages;

public class CommandValidator extends AbstractValidator<String> {
    /**
     * Basic filtering to help avoid common injection attempts. ProcessBuilder
     * should prevent most of this by default even that is based on assumptions
     * (such as the ffmpeg binary being invoked directly and not via a script
     * wrapper). Still some things can still be dangerous even via ffmpeg (consider
     * ../ paths - could be used to encode files outside the program's scope).
     */
    private static final String[] ILLEGAL_ARGUMNETS = new String[] { //
            "..", //
            ";", //
            "&", //
            "|" //
    };

    @Override
    public Set<Message> validate(String instance) {
        Set<Message> messages = this.valid();
        if (StringUtils.containsAny(instance, CommandValidator.ILLEGAL_ARGUMNETS)) {
            messages.add(this.toMessage(Messages.Application.InvalidField, this.getFieldVar("command")));
            return messages;
        }
        return messages;
    }
}
