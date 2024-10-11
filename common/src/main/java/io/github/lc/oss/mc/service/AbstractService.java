package io.github.lc.oss.mc.service;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import io.github.lc.oss.commons.l10n.L10N;
import io.github.lc.oss.commons.l10n.UserLocale;
import io.github.lc.oss.commons.l10n.Variable;
import io.github.lc.oss.commons.serialization.JsonMessage;
import io.github.lc.oss.commons.serialization.Message;
import io.github.lc.oss.mc.api.Entity;
import io.github.lc.oss.mc.api.ServiceResponse;

public abstract class AbstractService {
    private static final Logger logger = LoggerFactory.getLogger(AbstractService.class);

    @Autowired
    private L10N l10n;
    @Autowired
    private UserLocale userLocale;

    protected <T extends Entity> void addMessage(ServiceResponse<T> response, Message message, Variable... vars) {
        if (response.getMessages() == null) {
            response.setMessages(new HashSet<>());
        }
        response.getMessages().add(this.toMessage(message, vars));
    }

    protected Set<Message> toMessages(Message... messages) {
        return Arrays.stream(messages). //
                map(m -> this.toMessage(m)).//
                collect(Collectors.toSet());
    }

    protected Message toMessage(Message message, Variable... vars) {
        return new JsonMessage( //
                message.getCategory(), //
                message.getSeverity(), //
                message.getNumber(), //
                this.getText(message, vars));
    }

    protected String getText(Message message, Variable... vars) {
        return this.getL10n().getText(this.getUserLocale(), String.format( //
                "messages.%s.%s.%d", //
                message.getCategory(), //
                message.getSeverity(), //
                message.getNumber()), //
                vars);
    }

    protected Variable getFieldVar(String field) {
        return new Variable("Field", field == null ? "" : this.getL10n().getText(this.getUserLocale(), field));
    }

    protected L10N getL10n() {
        return this.l10n;
    }

    protected Locale getUserLocale() {
        return this.userLocale.getLocale();
    }

    protected Logger getLogger() {
        return AbstractService.logger;
    }
}
