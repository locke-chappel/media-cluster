package io.github.lc.oss.mc.scheduler.app.validation;

import java.util.Set;
import java.util.regex.Pattern;

import org.springframework.stereotype.Component;

import io.github.lc.oss.commons.serialization.Message;
import io.github.lc.oss.mc.api.Messages;
import io.github.lc.oss.mc.validation.Patterns;
import io.github.lc.oss.mc.scheduler.app.model.Credentials;

@Component
public class CredentialsValidator extends AbstractValidator<Credentials> {
    private static final Pattern PASSWORD = Pattern.compile("^.{1,256}$");

    @Override
    public Set<Message> validate(Credentials instance) {
        Set<Message> messages = this.valid();
        if (this.missingValue(instance)) {
            messages.add(this.toMessage(Messages.Authentication.InvalidCredentials));
            return messages;
        }

        if (!this.matches(Patterns.Username, instance.getUsername())) {
            messages.add(this.toMessage(Messages.Authentication.InvalidCredentials));
            return messages;
        }

        if (!this.matches(CredentialsValidator.PASSWORD, instance.getPassword())) {
            messages.add(this.toMessage(Messages.Authentication.InvalidCredentials));
            return messages;
        }

        return messages;
    }
}
