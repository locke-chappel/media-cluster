package io.github.lc.oss.mc.validation;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatcher;
import org.mockito.ArgumentMatchers;
import org.mockito.Mock;
import org.mockito.Mockito;

import io.github.lc.oss.commons.encoding.Encodings;
import io.github.lc.oss.commons.l10n.L10N;
import io.github.lc.oss.commons.l10n.UserLocale;
import io.github.lc.oss.commons.serialization.Message;
import io.github.lc.oss.commons.signing.Algorithms;
import io.github.lc.oss.mc.AbstractMockTest;
import io.github.lc.oss.mc.api.Messages;
import io.github.lc.oss.mc.api.SignedRequest;

public class AbstractSignedRequestValidatorTest extends AbstractMockTest {
    private static final String PUBLIC_KEY = "MCowBQYDK2VwAyEA/nhJdAGmoLecvJlhjv6KpWGeS5EBtD3jE873wopqEAA=";
    private static final String PRIVATE_KEY = "MC4CAQAwBQYDK2VwBCIEIJJw6OfOvokC4xO+t247uiR/3ffxrz0i8EBFRH0NWFHs";

    private static class TestValidator extends AbstractSignedRequestValidator<SignedRequest> {
        @Override
        public Set<Message> validate(SignedRequest instance) {
            return null;
        }

        @Override
        protected String getPublicKey(String nodeId) {
            return AbstractSignedRequestValidatorTest.PUBLIC_KEY;
        }
    }

    @Mock
    private L10N l10n;
    @Mock
    private UserLocale userLocale;

    private AbstractSignedRequestValidator<SignedRequest> validator;

    @BeforeEach
    public void init() {
        this.validator = new TestValidator();

        this.setField("l10n", this.l10n, this.validator);
        this.setField("userLocale", this.userLocale, this.validator);

        this.setField("maxRequestAge", null, this.validator);
        this.setField("maxRequestFuture", null, this.validator);
    }

    @Test
    public void test_getMaxRequestAge() {
        Long value = this.getField("maxRequestAge", this.validator);
        Assertions.assertNull(value);

        long result = this.validator.getMaxRequestAge();
        Assertions.assertEquals(30 * 1000, result);

        this.setField("maxRequestAge", 10l, this.validator);
        result = this.validator.getMaxRequestAge();
        Assertions.assertEquals(10, result);
    }

    @Test
    public void test_getMaxRequestFutureAge() {
        Long value = this.getField("maxRequestFuture", this.validator);
        Assertions.assertNull(value);

        long result = this.validator.getMaxRequestFutureAge();
        Assertions.assertEquals(5 * 1000, result);

        this.setField("maxRequestFuture", 100l, this.validator);
        result = this.validator.getMaxRequestFutureAge();
        Assertions.assertEquals(100, result);
    }

    @Test
    public void test_validateBasic_noRequest() {
        this.expectMessage(Messages.Application.RequiredFieldMissing);

        Set<Message> result = this.validator.validateBasic(null);
        this.assertMessage(result, Messages.Application.RequiredFieldMissing);
    }

    @Test
    public void test_validateBasic_noValues() {
        this.expectMessage(Messages.Application.RequiredFieldMissing);

        SignedRequest request = new SignedRequest();

        Set<Message> result = this.validator.validateBasic(request);
        this.assertMessage(result, Messages.Application.RequiredFieldMissing);
    }

    @Test
    public void validateBasic_valid() {
        SignedRequest request = new SignedRequest();
        request.setNodeId("junit");
        request.setBody(Encodings.Base64.encode("{}"));
        request.setSignature(Algorithms.ED25519.getSignature(AbstractSignedRequestValidatorTest.PRIVATE_KEY,
                request.getSignatureData()));

        Set<Message> result = this.validator.validateBasic(request);
        Assertions.assertNotNull(result);
        Assertions.assertTrue(result.isEmpty());
    }

    @Test
    public void test_validateCreation_expired() {
        this.expectMessage(Messages.Authentication.ExpiredRequest, false);

        SignedRequest request = new SignedRequest();
        request.setCreated(System.currentTimeMillis() - 35 * 1000);

        Set<Message> result = this.validator.validateCreation(request);
        this.assertMessage(result, Messages.Authentication.ExpiredRequest);
    }

    @Test
    public void test_validateCreation_tooFuture() {
        this.expectMessage(Messages.Authentication.ExpiredRequest, false);

        SignedRequest request = new SignedRequest();
        request.setCreated(System.currentTimeMillis() + 10 * 1000);

        Set<Message> result = this.validator.validateCreation(request);
        this.assertMessage(result, Messages.Authentication.ExpiredRequest);
    }

    @Test
    public void test_validateCreation_valid() {
        SignedRequest request = new SignedRequest();
        request.setCreated(System.currentTimeMillis() - 10 * 1000);

        Set<Message> result = this.validator.validateCreation(request);
        Assertions.assertNotNull(result);
        Assertions.assertTrue(result.isEmpty());
    }

    @Test
    public void test_validateSignature_invalidSig() {
        this.expectMessage(Messages.Authentication.InvalidSignature, false);

        SignedRequest request = new SignedRequest();
        request.setSignature("junk");

        Set<Message> result = this.validator.validateSignature(request);
        this.assertMessage(result, Messages.Authentication.InvalidSignature);
    }

    private void expectMessage(Message message) {
        this.expectMessage(message, true);
    }

    private void expectMessage(Message message, boolean withVars) {
        Mockito.when(this.userLocale.getLocale()).thenReturn(Locale.ENGLISH);
        if (withVars) {
            Mockito.when(this.l10n.getText(ArgumentMatchers.eq(Locale.ENGLISH), ArgumentMatchers.argThat( //
                    new ArgumentMatcher<String>() {
                        private static final Set<String> ALLOWED_FIELDS = Collections
                                .unmodifiableSet(new HashSet<>(Arrays.asList( //
                                        "SignedRequest", //
                                        "SignedRequest.Signature" //
                        )));

                        @Override
                        public boolean matches(String argument) {
                            return ALLOWED_FIELDS.contains(argument);
                        }
                    }))).thenReturn("field-value");
            Mockito.when(this.l10n.getText( //
                    ArgumentMatchers.eq(Locale.ENGLISH), //
                    ArgumentMatchers.eq(String.format( //
                            "messages.%s.%s.%d", //
                            message.getCategory(), //
                            message.getSeverity(), //
                            message.getNumber())), //
                    ArgumentMatchers.notNull())).thenReturn("Message");
        } else {
            Mockito.when(this.l10n.getText( //
                    ArgumentMatchers.eq(Locale.ENGLISH), //
                    ArgumentMatchers.eq(String.format( //
                            "messages.%s.%s.%d", //
                            message.getCategory(), //
                            message.getSeverity(), //
                            message.getNumber()))))
                    .thenReturn("Message");
        }

    }

    private void assertMessage(Collection<Message> messages, Message message) {
        Assertions.assertNotNull(messages);
        Assertions.assertEquals(1, messages.size());
        Message actual = messages.iterator().next();
        Assertions.assertNotNull(actual);
        Assertions.assertEquals(message.getCategory(), actual.getCategory());
        Assertions.assertEquals(message.getSeverity(), actual.getSeverity());
        Assertions.assertEquals(message.getNumber(), actual.getNumber());
        Assertions.assertEquals("Message", actual.getText());
    }
}
