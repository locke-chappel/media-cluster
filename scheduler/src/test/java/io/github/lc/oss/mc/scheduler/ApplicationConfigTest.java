package io.github.lc.oss.mc.scheduler;

import java.sql.SQLException;

import org.h2.tools.Server;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.context.ApplicationContext;
import org.springframework.core.env.Environment;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;

public class ApplicationConfigTest extends AbstractMockTest {
    @Test
    public void test_inMemoryDatabase() {
        ApplicationConfig config = new ApplicationConfig();
        Server result = null;

        this.setField("enableDatabaseServer", false, config);
        try {
            result = config.inMemoryDatabase();
        } catch (SQLException e) {
            Assertions.fail("Unexpected exception");
        }
        Assertions.assertNull(result);

        this.setField("enableDatabaseServer", true, config);
        try {
            result = config.inMemoryDatabase();
        } catch (SQLException e) {
            Assertions.fail("Unexpected exception");
        }
        Assertions.assertNotNull(result);
    }

    @Test
    public void test_scheduler_exception() {
        ApplicationContext context = Mockito.mock(ApplicationContext.class);
        Environment env = Mockito.mock(Environment.class);

        Mockito.when(context.getEnvironment()).thenReturn(env);
        Mockito.when(env.getProperty("integrationtest", Boolean.class, Boolean.FALSE)).thenReturn(false);
        Mockito.when(env.getProperty("application.jobs.NodeInformJob.enabled", Boolean.class, Boolean.FALSE))
                .thenReturn(false);

        ApplicationConfig config = new ApplicationConfig();

        try {
            config.scheduler(context);
            Assertions.fail("Expected exception");
        } catch (RuntimeException ex) {
            Assertions.assertEquals("Error initalizaing Quarts Scheduler", ex.getMessage());
            Assertions.assertEquals("Error scheduling job", ex.getCause().getMessage());
        }
    }

    @Test
    public void test_scheduler_withoutNodeInformJob() {
        ApplicationContext context = Mockito.mock(ApplicationContext.class);
        Environment env = Mockito.mock(Environment.class);

        Mockito.when(env.getProperty("integrationtest", Boolean.class, Boolean.FALSE)).thenReturn(false);
        Mockito.when(env.getProperty("application.jobs.NodeInformJob.enabled", Boolean.class, Boolean.FALSE))
                .thenReturn(false);

        Mockito.when(context.getEnvironment()).thenReturn(env);
        Mockito.when(env.getRequiredProperty("application.jobs.CsrfTokenSaltJob")).thenReturn("0 */5 * ? * *");
        Mockito.when(env.getRequiredProperty("application.jobs.JobHistoryCleanupJob")).thenReturn("0 */5 * ? * *");
        Mockito.when(env.getRequiredProperty("application.jobs.NodeStatusUpdateJob")).thenReturn("0 */5 * ? * *");
        Mockito.when(env.getRequiredProperty("application.jobs.TokenCleanupJob")).thenReturn("0 */5 * ? * *");

        ApplicationConfig config = new ApplicationConfig();
        config.scheduler(context);
    }

    @Test
    public void test_scheduler_withNodeInformJob() {
        ApplicationContext context = Mockito.mock(ApplicationContext.class);
        Environment env = Mockito.mock(Environment.class);

        Mockito.when(env.getProperty("integrationtest", Boolean.class, Boolean.FALSE)).thenReturn(false);
        Mockito.when(env.getProperty("application.jobs.NodeInformJob.enabled", Boolean.class, Boolean.FALSE))
                .thenReturn(true);

        Mockito.when(context.getEnvironment()).thenReturn(env);
        Mockito.when(env.getRequiredProperty("application.jobs.CsrfTokenSaltJob")).thenReturn("0 */5 * ? * *");
        Mockito.when(env.getRequiredProperty("application.jobs.JobHistoryCleanupJob")).thenReturn("0 */5 * ? * *");
        Mockito.when(env.getRequiredProperty("application.jobs.NodeStatusUpdateJob")).thenReturn("0 */5 * ? * *");
        Mockito.when(env.getRequiredProperty("application.jobs.TokenCleanupJob")).thenReturn("0 */5 * ? * *");
        Mockito.when(env.getRequiredProperty("application.jobs.NodeInformJob")).thenReturn("0 */5 * ? * *");

        ApplicationConfig config = new ApplicationConfig();
        config.scheduler(context);
    }

    @Test
    public void test_hasAnyPermission_null() {
        boolean result = ApplicationConfig.hasAnyPermission(null);
        Assertions.assertFalse(result);
    }

    @Test
    public void test_hasAnyPermission_wrongUserType() {
        boolean result = ApplicationConfig.hasAnyPermission(new UsernamePasswordAuthenticationToken(null, null));
        Assertions.assertFalse(result);
    }
}
