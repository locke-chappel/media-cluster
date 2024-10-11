package io.github.lc.oss.mc.scheduler.app.validation;

import java.util.HashSet;
import java.util.Set;

import org.springframework.stereotype.Component;

import io.github.lc.oss.commons.serialization.Message;
import io.github.lc.oss.mc.api.Messages;
import io.github.lc.oss.mc.validation.Patterns;
import io.github.lc.oss.mc.scheduler.app.model.User;

@Component
public class UserValidator extends AbstractValidator<User> {
    @Override
    public Set<Message> validate(User instance) {
        Set<Message> messages = new HashSet<>();
        if (this.missingValue(instance)) {
            messages.add(this.toMessage(Messages.Application.RequiredFieldMissing,
                    this.getFieldVar("settings.user.header")));
            return messages;
        }

        messages.addAll(this.validateString("settings.user.username", Patterns.Username, true, instance.getUsername()));

        if (this.missingValue(instance.getPassword())) {
            messages.add(this.toMessage(Messages.Application.RequiredFieldMissing,
                    this.getFieldVar("settings.user.password")));
        }

        if (this.missingValue(instance.getConfirm())) {
            messages.add(this.toMessage(Messages.Application.RequiredFieldMissing,
                    this.getFieldVar("settings.user.confirm")));
        }

        if (!messages.isEmpty()) {
            return messages;
        }

        if (!instance.getPassword().equals(instance.getConfirm())) {
            messages.add(this.toMessage(Messages.Application.PasswordMismatch));
        }

        return messages;
    }
}
