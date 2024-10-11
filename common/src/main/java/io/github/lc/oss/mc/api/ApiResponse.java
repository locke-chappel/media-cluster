package io.github.lc.oss.mc.api;

import java.util.Collection;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

import io.github.lc.oss.commons.serialization.Jsonable;

@JsonInclude(Include.NON_EMPTY)
public class ApiResponse implements Jsonable {
    private Collection<Messages> messages;

    public Collection<Messages> getMessages() {
        return this.messages;
    }

    public void setMessages(Collection<Messages> messages) {
        this.messages = messages;
    }
}
