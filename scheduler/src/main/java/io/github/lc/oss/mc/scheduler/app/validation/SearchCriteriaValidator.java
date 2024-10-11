package io.github.lc.oss.mc.scheduler.app.validation;

import java.util.HashSet;
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import io.github.lc.oss.commons.jpa.SearchCriteria;
import io.github.lc.oss.commons.jpa.Term;
import io.github.lc.oss.commons.serialization.Message;
import io.github.lc.oss.mc.api.Messages;
import io.github.lc.oss.mc.entity.Constants;

@Component
public class SearchCriteriaValidator extends AbstractValidator<SearchCriteria> {
    @Autowired
    private SearchTermValidator termValidator;

    @Override
    public Set<Message> validate(SearchCriteria criteria) {
        Set<Message> messages = new HashSet<>();
        if (this.missingValue(criteria)) {
            return this.valid();
        }

        if (criteria.getPageSize() < Constants.Lengths.Search.MIN_PAGE_SIZE || //
                criteria.getPageSize() > Constants.Lengths.Search.MAX_PAGE_SIZE) {
            messages.add(this.toMessage(Messages.Application.InvalidPageSize));
        }

        if (criteria.getPageNumber() < 0) {
            messages.add(this.toMessage(Messages.Application.InvalidPageNumber));
        }

        for (Term term : criteria.getSearchTerms()) {
            messages.addAll(this.termValidator.validate(term));
        }

        return messages;
    }
}
