package io.github.lc.oss.mc.api;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

@JsonInclude(Include.NON_EMPTY)
public class NodeStatus extends Node {
    private Job currentJob;

    public Job getCurrentJob() {
        return this.currentJob;
    }

    public void setCurrentJob(Job currentJob) {
        this.currentJob = currentJob;
    }
}
