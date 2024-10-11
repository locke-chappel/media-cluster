package io.github.lc.oss.mc.scheduler.app.validation;

import java.util.Arrays;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;

import io.github.lc.oss.commons.serialization.Message;
import io.github.lc.oss.mc.api.Messages;
import io.github.lc.oss.mc.scheduler.app.model.Node;
import io.github.lc.oss.mc.scheduler.app.model.NodeTypes;
import io.github.lc.oss.mc.scheduler.app.repository.NodeRepository;

public class NodeValidatorTest extends AbstractValidatorTest {
    @Mock
    private NodeRepository nodeRepo;

    @InjectMocks
    private NodeValidator validator;

    @Test
    public void test_null() {
        this.expectLocale();
        this.expectMessage(Messages.Application.RequiredFieldMissing);
        this.expectFieldVar("nodes.node.header");

        Set<Message> result = this.validator.validate(null);
        this.assertMessage(Messages.Application.RequiredFieldMissing, result);
    }

    @Test
    public void test_missingValues() {
        this.expectLocale();
        this.expectMessage(Messages.Application.RequiredFieldMissing);
        this.expectFieldVar("nodes.node.name");
        this.expectFieldVar("nodes.node.url");

        Node node = new Node();

        Set<Message> result = this.validator.validate(node);

        this.assertMessages(Arrays.asList( //
                Messages.Application.RequiredFieldMissing, //
                Messages.Application.RequiredFieldMissing), result);
    }

    @Test
    public void test_duplicate() {
        this.expectLocale();
        this.expectMessage(Messages.Application.DuplicateNodeName);
        this.expectMessage(Messages.Application.InvalidField);
        this.expectFieldVar("nodes.node.clusterName");

        Node node = new Node();
        node.setUrl("https://junit-node");
        node.setName("junit-node");
        node.setClusterName("#a b");

        io.github.lc.oss.mc.scheduler.app.entity.Node other = new io.github.lc.oss.mc.scheduler.app.entity.Node();
        other.setName(node.getName());
        this.setField("id", "id-2", other);

        Mockito.when(this.nodeRepo.findByNameIgnoreCase(node.getName())).thenReturn(other);

        Set<Message> result = this.validator.validate(node);

        this.assertMessages(Arrays.asList( //
                Messages.Application.DuplicateNodeName, //
                Messages.Application.InvalidField), result);
    }

    @Test
    public void test_edit_invalid() {
        this.expectLocale();
        this.expectMessage(Messages.Application.InvalidField);
        this.expectFieldVar("nodes.node.clusterName");

        Node node = new Node();
        node.setUrl("https://junit-node");
        node.setName("junit-node");
        node.setClusterName("#a b");
        node.setId("id");

        io.github.lc.oss.mc.scheduler.app.entity.Node exiting = new io.github.lc.oss.mc.scheduler.app.entity.Node();
        exiting.setName(node.getName());
        exiting.setType(NodeTypes.Worker);
        this.setField("id", node.getId(), exiting);

        Mockito.when(this.nodeRepo.findByNameIgnoreCase(node.getName())).thenReturn(exiting);
        Mockito.when(this.nodeRepo.findById(node.getId())).thenReturn(Optional.of(exiting));

        Set<Message> result = this.validator.validate(node);

        this.assertMessage(Messages.Application.InvalidField, result);
    }

    @Test
    public void test_scheduler_valid() {
        Node node = new Node();
        node.setUrl("https://junit-scheduler");
        node.setName("junit-scheduler");
        node.setClusterName(null);
        node.setId("id");

        io.github.lc.oss.mc.scheduler.app.entity.Node exiting = new io.github.lc.oss.mc.scheduler.app.entity.Node();
        exiting.setName(node.getName());
        exiting.setType(NodeTypes.Scheduler);
        this.setField("id", node.getId(), exiting);

        Mockito.when(this.nodeRepo.findByNameIgnoreCase(node.getName())).thenReturn(exiting);
        Mockito.when(this.nodeRepo.findById(node.getId())).thenReturn(Optional.of(exiting));

        Set<Message> result = this.validator.validate(node);
        this.assertValid(result);
    }

    @Test
    public void test_worker_edit_valid() {
        Node node = new Node();
        node.setUrl("https://junit-node");
        node.setName("junit-node");
        node.setClusterName("cluster");
        node.setId("id");

        io.github.lc.oss.mc.scheduler.app.entity.Node exiting = new io.github.lc.oss.mc.scheduler.app.entity.Node();
        exiting.setName(node.getName());
        exiting.setType(NodeTypes.Worker);
        this.setField("id", node.getId(), exiting);

        Mockito.when(this.nodeRepo.findByNameIgnoreCase(node.getName())).thenReturn(exiting);
        Mockito.when(this.nodeRepo.findById(node.getId())).thenReturn(Optional.of(exiting));

        Set<Message> result = this.validator.validate(node);
        this.assertValid(result);
    }

    @Test
    public void test_worker_new_valid() {
        Node node = new Node();
        node.setUrl("https://junit-node");
        node.setName("junit-node");
        node.setClusterName("cluster");

        Mockito.when(this.nodeRepo.findByNameIgnoreCase(node.getName())).thenReturn(null);

        Set<Message> result = this.validator.validate(node);
        this.assertValid(result);
    }
}
