package io.github.lc.oss.mc.validation;

import java.util.Set;
import java.util.regex.Pattern;

import org.springframework.beans.factory.annotation.Autowired;

import io.github.lc.oss.commons.l10n.L10N;
import io.github.lc.oss.commons.l10n.UserLocale;
import io.github.lc.oss.commons.l10n.Variable;
import io.github.lc.oss.commons.serialization.JsonMessage;
import io.github.lc.oss.commons.serialization.Message;
import io.github.lc.oss.mc.api.Messages;

public abstract class AbstractValidator<Type> extends io.github.lc.oss.commons.validation.AbstractValidator<Type> {
    @Autowired
    private L10N l10n;
    @Autowired
    private UserLocale userLocale;

    protected void validateString(Set<Message> messages, String fieldName, Pattern validationPattern, boolean required,
            String value) {
        messages.addAll(this.validateString(fieldName, validationPattern, required, value));
    }

    protected Set<Message> validateString(String fieldName, Pattern validationPattern, boolean required, String value) {
        Set<Message> messages = this.valid();
        if (this.missingValue(value)) {
            if (required) {
                messages.add(this.toMessage(Messages.Application.RequiredFieldMissing, this.getFieldVar(fieldName)));
            }
        } else if (!this.matches(validationPattern, value)) {
            messages.add(this.toMessage(Messages.Application.InvalidField, this.getFieldVar(fieldName)));
        }
        return messages;
    }

    protected Message toMessage(Message message, Variable... vars) {
        return new JsonMessage( //
                message.getCategory(), //
                message.getSeverity(), //
                message.getNumber(), //
                this.getText(message, vars));
    }

    protected String getText(Message message, Variable... vars) {
        return this.getL10n().getText(this.getUserLocale().getLocale(), String.format( //
                "messages.%s.%s.%d", //
                message.getCategory(), //
                message.getSeverity(), //
                message.getNumber()), //
                vars);
    }

    protected Variable getFieldVar(String field) {
        return new Variable("Field",
                field == null ? "" : this.getL10n().getText(this.getUserLocale().getLocale(), field));
    }

    protected L10N getL10n() {
        return this.l10n;
    }

    protected UserLocale getUserLocale() {
        return this.userLocale;
    }
}
