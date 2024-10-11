package io.github.lc.oss.mc.scheduler.app.controllers.v1;

import org.junit.jupiter.api.Test;

import io.github.lc.oss.commons.serialization.Message.Severities;
import io.github.lc.oss.mc.scheduler.AbstractSeleniumTest;

public class PublicControllerUiIT extends AbstractSeleniumTest {
    @Test
    public void test_public_pages() {
        // --- open app
        this.navigate("/");

        this.sendKeys("txtUsername", "junk");
        this.sendKeys("txtPassword", "junk");
        this.clickById("btnLogin");

        this.assertMessage(Severities.Error, "Invalid username/password");

        this.sendKeys("txtUsername", this.getDefaultUsername());
        this.sendKeys("txtPassword", this.getDefaultUserPassword());
        this.clickById("btnLogin");

        this.waitForNavigate("/");

        this.logout();

        this.waitForNavigate("/login");

        // --- cause error
        this.navigate("/error");
        this.waitForNavigate("/error");
        this.assertTextContent("content", "Oops...something unexpected happened.");
    }

    @Test
    public void test_unauthenticated() {
        // --- jobs (default) page
        this.navigate("/");
        this.waitForNavigate("/login");

        // --- nodes page
        this.navigate("/nodes");
        this.waitForNavigate("/login");

        // --- settings page
        this.navigate("/settings");
        this.waitForNavigate("/login");
    }
}
