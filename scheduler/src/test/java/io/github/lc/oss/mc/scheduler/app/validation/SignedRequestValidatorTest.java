package io.github.lc.oss.mc.scheduler.app.validation;

import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;

import io.github.lc.oss.commons.serialization.Message;
import io.github.lc.oss.mc.api.Job;
import io.github.lc.oss.mc.api.Messages;
import io.github.lc.oss.mc.api.SignedRequest;
import io.github.lc.oss.mc.scheduler.app.entity.Node;
import io.github.lc.oss.mc.scheduler.app.repository.NodeRepository;

public class SignedRequestValidatorTest extends AbstractValidatorTest {
    @Mock
    private NodeRepository nodeRepo;

    @InjectMocks
    private SignedRequestValidator validator;

    @Test
    public void test_getPublicKey_blanks() {
        String result = this.validator.getPublicKey(null);
        Assertions.assertNull(result);

        result = this.validator.getPublicKey("");
        Assertions.assertNull(result);

        result = this.validator.getPublicKey(" \t \r \n \t ");
        Assertions.assertNull(result);
    }

    @Test
    public void test_getPublicKey_nodeNotFound() {
        Mockito.when(this.nodeRepo.findById("junk")).thenReturn(Optional.empty());

        String result = this.validator.getPublicKey("junk");
        Assertions.assertNull(result);
    }

    @Test
    public void test_getPublicKey() {
        Node worker = this.fac().worker("w1");

        Mockito.when(this.nodeRepo.findById("junk")).thenReturn(Optional.of(worker));

        String result = this.validator.getPublicKey("junk");
        Assertions.assertEquals(worker.getPublicKey(), result);
    }

    @Test
    public void test_validate_null() {
        this.expectLocale();
        this.expectMessage(Messages.Application.RequiredFieldMissing);
        this.expectFieldVar("SignedRequest");

        Set<Message> result = this.validator.validate(null);
        this.assertMessage(Messages.Application.RequiredFieldMissing, result);
    }

    @Test
    public void test_validate_invalidNode() {
        this.expectLocale();
        this.expectMessage(Messages.Application.InvalidNode);

        Job body = new Job();
        SignedRequest request = this.fac().sign("node-id", body);

        Mockito.when(this.nodeRepo.findById(request.getNodeId())).thenReturn(Optional.empty());

        Set<Message> result = this.validator.validate(request);
        this.assertMessage(Messages.Application.InvalidNode, result);
    }

    @Test
    public void test_validate_invalidSignature() {
        this.expectLocale();
        this.expectMessage(Messages.Authentication.InvalidSignature);

        Job body = new Job();
        SignedRequest request = this.fac().sign("node-id", body);

        Node worker = this.fac().worker("w1");
        worker.setPublicKey("MCowBQYDK2VwAyEACS+qyMd2h4GYkT/kHNerZHmC88asZYOB52yY8k/3YB0=");
        Mockito.when(this.nodeRepo.findById(request.getNodeId())).thenReturn(Optional.of(worker));

        Set<Message> result = this.validator.validate(request);
        this.assertMessage(Messages.Authentication.InvalidSignature, result);
    }

    @Test
    public void test_validate_valid() {
        Job body = new Job();
        SignedRequest request = this.fac().sign("node-id", body);

        Node worker = this.fac().worker("w1");
        Mockito.when(this.nodeRepo.findById(request.getNodeId())).thenReturn(Optional.of(worker));

        Set<Message> result = this.validator.validate(request);
        this.assertValid(result);
    }
}
