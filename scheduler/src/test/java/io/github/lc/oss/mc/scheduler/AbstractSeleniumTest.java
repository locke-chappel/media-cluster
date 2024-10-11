package io.github.lc.oss.mc.scheduler;

import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.Rollback;
import org.springframework.test.context.ActiveProfiles;

import io.github.lc.oss.commons.serialization.Message.Severities;

@SpringBootTest(classes = { IntegrationConfig.class })
@Rollback
@Tag("seleniumTest")
@ActiveProfiles("integrationtest")
public abstract class AbstractSeleniumTest extends io.github.lc.oss.commons.testing.web.AbstractSeleniumTest {
    @Autowired
    private Factory factory;
    @Autowired
    private SqlHelper sqlHelper;

    @BeforeEach
    public void dbCleanup() {
        this.sqlHelper.clearDatabase();
        this.getFactory().initDatabase();
    }

    protected WebElement findByCssSelector(WebElement root, String selector) {
        return this.waitUntil(ExpectedConditions.elementToBeClickable(root.findElement(By.cssSelector(selector))));
    }

    protected void assertMessage(Severities severity, String text) {
        this.assertMessage(severity.name().toLowerCase(), text);
    }

    protected void clickMenu() {
        this.clickById("btnMenu");
        this.waitFor(250);
    }

    protected void logout() {
        this.logout("btnSignOutHeader");
    }

    protected void logoutSideMenu() {
        this.logout("btnSignOut");
    }

    protected void logout(String id) {
        if (StringUtils.equals("btnSignOut", id)) {
            this.clickMenu();
        }
        this.clickById(id);
    }

    public String getDefaultUsername() {
        return this.getFactory().getDefaultUsername();
    }

    public String getDefaultUserPassword() {
        return this.getFactory().getDefaultUserPassword();
    }

    protected Factory getFactory() {
        return this.factory;
    }
}
