package io.github.lc.oss.mc.worker;

import org.springframework.boot.builder.SpringApplicationBuilder;

public final class Application {
    public static void main(String[] args) {
        SpringApplicationBuilder builder = new SpringApplicationBuilder(ApplicationConfig.class);
        builder.run(args);
    }

    private Application() {

    }
}
