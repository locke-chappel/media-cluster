package io.github.lc.oss.mc.scheduler;

import org.apache.catalina.Context;
import org.apache.tomcat.util.http.Rfc6265CookieProcessor;
import org.springframework.boot.web.embedded.tomcat.TomcatContextCustomizer;
import org.springframework.boot.web.embedded.tomcat.TomcatServletWebServerFactory;
import org.springframework.boot.web.server.WebServerFactoryCustomizer;

public class SameSiteCustomizer implements WebServerFactoryCustomizer<TomcatServletWebServerFactory> {
    @Override
    public void customize(TomcatServletWebServerFactory factory) {
        factory.addContextCustomizers(new TomcatContextCustomizer() {
            @Override
            public void customize(Context context) {
                Rfc6265CookieProcessor processor = new Rfc6265CookieProcessor();
                processor.setSameSiteCookies("strict");
                context.setCookieProcessor(processor);
            }
        });
    }
}
