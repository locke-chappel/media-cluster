package io.github.lc.oss.mc.worker;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.context.ApplicationContext;

import io.github.lc.oss.commons.util.AbstractConsoleListener;

import jakarta.annotation.PostConstruct;

public class ConsoleListener extends AbstractConsoleListener {
    @Autowired
    private ApplicationContext context;

    @PostConstruct
    public void init() {
        this.start();
    }

    @Override
    protected void process(String line) {
        if (StringUtils.equals("exit", line)) {
            System.out.println("Shutting down...");
            SpringApplication.exit(this.context);
        }
    }
}
