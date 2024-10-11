package io.github.lc.oss.mc.worker.app.validation;

import java.util.Set;

import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mockito;

import io.github.lc.oss.commons.serialization.Message;
import io.github.lc.oss.mc.api.ApiObject;
import io.github.lc.oss.mc.api.Job;
import io.github.lc.oss.mc.api.Messages;
import io.github.lc.oss.mc.api.SignedRequest;

public class SignedJobRequestValidatorTest extends AbstractValidatorTest {
    @InjectMocks
    private SignedRequestValidator validator;

    @Test
    public void test_nullRequest() {
        this.expectLocale();
        this.expectMessage(Messages.Application.RequiredFieldMissing);
        this.expectFieldVar("SignedRequest");

        Set<Message> result = this.validator.validate(null);
        this.assertMessage(Messages.Application.RequiredFieldMissing, result);
    }

    @Test
    public void test_wrongNode() {
        this.expectLocale();
        this.expectMessage(Messages.Application.InvalidNode);
        this.expectPrivateKey();
        this.expectPublicKey();

        Mockito.when(this.getConfig().getId()).thenReturn("node-id");

        ApiObject request = new Job();

        SignedRequest signed = this.fac().sign("other-node", request);

        Set<Message> result = this.validator.validate(signed);
        this.assertMessage(Messages.Application.InvalidNode, result);
    }

    @Test
    public void test_noSignature() {
        this.expectLocale();
        this.expectMessage(Messages.Application.RequiredFieldMissing);
        this.expectFieldVar("SignedRequest.Signature");

        SignedRequest request = new SignedRequest();
        request.setSignature(null);

        Set<Message> result = this.validator.validate(request);
        this.assertMessage(Messages.Application.RequiredFieldMissing, result);
    }
}
