package io.github.lc.oss.mc.api;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import io.github.lc.oss.commons.serialization.Message;

public class ServiceResponse<T extends Entity> {
    private T entity;
    private Set<Message> messages;

    public T getEntity() {
        return this.entity;
    }

    public void setEntity(T entity) {
        this.entity = entity;
    }

    public void addMessages(Message... messages) {
        this.addMessages(Arrays.asList(messages));
    }

    public void addMessages(Collection<Message> messages) {
        if (this.messages == null) {
            this.messages = new HashSet<>();
        }
        this.messages.addAll(messages);
    }

    public Set<Message> getMessages() {
        return this.messages;
    }

    public void setMessages(Set<Message> messages) {
        this.messages = messages;
    }

    /**
     * Returns true if response has any of the messages.
     */
    public boolean hasMessages(Message... messages) {
        if (this.messages == null || this.messages.isEmpty()) {
            return false;
        }

        if (messages.length == 0) {
            return true;
        }

        return Arrays.stream(messages).anyMatch(find -> {
            return this.messages.stream().anyMatch(m -> m.isSame(find));
        });
    }
}
