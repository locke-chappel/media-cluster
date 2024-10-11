package io.github.lc.oss.mc.api;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

@JsonInclude(Include.NON_EMPTY)
public class JobResult implements ApiObject {
    private String id;
    private Long result;

    public String getId() {
        return this.id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public Long getResult() {
        return this.result;
    }

    public void setResult(Long result) {
        this.result = result;
    }
}
