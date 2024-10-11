package io.github.lc.oss.mc.scheduler.app.validation;

import java.util.Set;

import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;

import io.github.lc.oss.commons.jpa.Term;
import io.github.lc.oss.commons.serialization.Message;
import io.github.lc.oss.mc.api.Messages;

public class SearchTermValidatorTest extends AbstractValidatorTest {
    @InjectMocks
    private SearchTermValidator validator;

    @Test
    public void test_validate_invalid_type() {
        this.expectLocale();
        this.expectMessage(Messages.Application.InvalidSearchTerm);

        Term t = new Term("prop", new Object());

        Set<Message> result = this.validator.validate(t);
        this.assertMessage(Messages.Application.InvalidSearchTerm, result);
    }

    @Test
    public void test_validate_invalid_string() {
        this.expectLocale();
        this.expectMessage(Messages.Application.InvalidSearchTerm);

        Term t = new Term("prop", "Maximum search term length is 128 characters." + //
                "Maximum search term length is 128 characters." + //
                "Maximum search term length is 128 characters." + //
                "Maximum search term length is 128 characters.");

        Set<Message> result = this.validator.validate(t);
        this.assertMessage(Messages.Application.InvalidSearchTerm, result);
    }

    @Test
    public void test_validate_invalid_proprty() {
        this.expectLocale();
        this.expectMessage(Messages.Application.InvalidSearchTerm);

        Term t = new Term("Maximum search term length is 128 characters." + //
                "Maximum search term length is 128 characters." + //
                "Maximum search term length is 128 characters." + //
                "Maximum search term length is 128 characters.", //
                "value");

        Set<Message> result = this.validator.validate(t);
        this.assertMessage(Messages.Application.InvalidSearchTerm, result);
    }

    @Test
    public void test_validate_null() {
        Set<Message> result = this.validator.validate(null);
        this.assertValid(result);
    }

    @Test
    public void test_validate_number() {
        Term t = new Term("prop", 1);

        Set<Message> result = this.validator.validate(t);
        this.assertValid(result);
    }

    @Test
    public void test_validate_boolean() {
        Term t = new Term("prop", true);

        Set<Message> result = this.validator.validate(t);
        this.assertValid(result);
    }

    @Test
    public void test_validate_string() {
        Term t = new Term("prop", "value");

        Set<Message> result = this.validator.validate(t);
        this.assertValid(result);
    }

    @Test
    public void test_validate_valueNull() {
        Term t = new Term("prop", null);

        Set<Message> result = this.validator.validate(t);
        this.assertValid(result);
    }
}
