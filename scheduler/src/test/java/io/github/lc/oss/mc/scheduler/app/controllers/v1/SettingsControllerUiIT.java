package io.github.lc.oss.mc.scheduler.app.controllers.v1;

import org.junit.jupiter.api.Test;

import io.github.lc.oss.commons.serialization.Message.Severities;
import io.github.lc.oss.mc.scheduler.AbstractSeleniumTest;

public class SettingsControllerUiIT extends AbstractSeleniumTest {
    @Test
    public void test_updateUser() {
        // --- open app
        this.navigate("/");

        this.sendKeys("txtUsername", this.getDefaultUsername());
        this.sendKeys("txtPassword", this.getDefaultUserPassword());
        this.clickById("btnLogin");

        this.waitForNavigate("/");

        this.clickMenu();
        this.clickById("btnSettings");

        this.waitForNavigate("/settings");

        this.sendKeys("txtUsername", "junit");
        this.sendKeys("txtPassword", "drowssap");
        this.sendKeys("txtPasswordConfirm", "drowssap");

        this.clickById("btnCancelUser");
        this.clickById("dirtyModal_btnOk");
        this.assertTextValue("txtUsername", "user");
        this.assertTextValue("txtPassword", "");
        this.assertTextValue("txtPasswordConfirm", "");

        this.sendKeys("txtUsername", "junit");
        this.sendKeys("txtPassword", "drowssap");
        this.sendKeys("txtPasswordConfirm", "drowssap");
        this.clickById("btnSaveUser");

        this.logoutSideMenu();

        this.waitForNavigate("/login");

        this.sendKeys("txtUsername", this.getDefaultUsername());
        this.sendKeys("txtPassword", this.getDefaultUserPassword());
        this.clickById("btnLogin");

        this.assertMessage(Severities.Error, "Invalid username/password");

        this.sendKeys("txtUsername", "junit");
        this.sendKeys("txtPassword", "drowssap");
        this.clickById("btnLogin");

        this.waitForNavigate("/");

        this.logout();

        this.waitForNavigate("/login");
    }
}
