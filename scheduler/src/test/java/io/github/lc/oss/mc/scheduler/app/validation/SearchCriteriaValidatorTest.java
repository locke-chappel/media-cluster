package io.github.lc.oss.mc.scheduler.app.validation;

import java.util.HashSet;
import java.util.Set;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;

import io.github.lc.oss.commons.jpa.SearchCriteria;
import io.github.lc.oss.commons.jpa.Term;
import io.github.lc.oss.commons.serialization.Message;
import io.github.lc.oss.mc.api.Messages;

public class SearchCriteriaValidatorTest extends AbstractValidatorTest {
    @Mock
    private SearchTermValidator termValidator;

    @InjectMocks
    private SearchCriteriaValidator validator;

    @Test
    public void test_validate_invalid() {
        this.expectLocale();
        this.expectMessage(Messages.Application.InvalidPageSize);
        this.expectMessage(Messages.Application.InvalidPageNumber);

        SearchCriteria sc = new SearchCriteria(-1, -1);

        Set<Message> result = this.validator.validate(sc);
        Assertions.assertNotNull(result);
        Assertions.assertEquals(2, result.size());
        Message sizeMessage = result.stream()
                .filter(m -> m.getNumber() == Messages.Application.InvalidPageSize.getNumber()).findAny().orElse(null);
        Message numberMessage = result.stream()
                .filter(m -> m.getNumber() == Messages.Application.InvalidPageNumber.getNumber()).findAny()
                .orElse(null);

        this.assertMessage(Messages.Application.InvalidPageSize, sizeMessage);
        this.assertMessage(Messages.Application.InvalidPageNumber, numberMessage);
    }

    @Test
    public void test_validate_invalid_v2() {
        this.expectLocale();
        this.expectMessage(Messages.Application.InvalidPageSize);

        SearchCriteria sc = new SearchCriteria(100000, 0);

        Set<Message> result = this.validator.validate(sc);
        this.assertMessage(Messages.Application.InvalidPageSize, result);
    }

    @Test
    public void test_validate_null() {
        Set<Message> result = this.validator.validate(null);
        this.assertValid(result);
    }

    @Test
    public void test_validate() {
        Mockito.when(this.termValidator.validate(ArgumentMatchers.notNull())).thenReturn(new HashSet<>());

        SearchCriteria sc = new SearchCriteria(10, 0, Term.of(new Term("property", "value")));

        Set<Message> result = this.validator.validate(sc);
        this.assertValid(result);
    }
}
