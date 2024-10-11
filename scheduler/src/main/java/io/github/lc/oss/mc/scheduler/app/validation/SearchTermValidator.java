package io.github.lc.oss.mc.scheduler.app.validation;

import java.util.HashSet;
import java.util.Set;

import org.springframework.stereotype.Component;

import io.github.lc.oss.commons.jpa.Term;
import io.github.lc.oss.commons.serialization.Message;
import io.github.lc.oss.mc.api.Messages;
import io.github.lc.oss.mc.validation.Patterns;

@Component
public class SearchTermValidator extends AbstractValidator<Term> {
    @Override
    public Set<Message> validate(Term term) {
        if (this.missingValue(term)) {
            return this.valid();
        }

        if (!this.matches(Patterns.SearchTerm, term.getProperty())) {
            Set<Message> messages = new HashSet<>();
            messages.add(this.toMessage(Messages.Application.InvalidSearchTerm));
            return messages;
        }

        if (term.getValue() == null) {
            return this.valid();
        }

        if (term.getValue() instanceof Number) {
            return this.valid();
        }

        if (term.getValue() instanceof Boolean) {
            return this.valid();
        }

        if (term.getValue() instanceof String && this.matches(Patterns.SearchTerm, (String) term.getValue())) {
            return this.valid();
        }

        Set<Message> messages = new HashSet<>();
        messages.add(this.toMessage(Messages.Application.InvalidSearchTerm));
        return messages;
    }
}
