package io.github.lc.oss.mc.scheduler;

import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.springframework.context.ApplicationContext;

public class ConsoleListenerTest extends AbstractMockTest {
    @Mock
    private ApplicationContext context;

    /*
     * This class is a testing helper with minimal logic, all we need to do is cover
     * the non-exit case and we are done.
     */
    @Test
    public void test_codeCoverage() {
        Thread thread = Mockito.mock(Thread.class);
        ConsoleListener listener = new ConsoleListener() {
            @Override
            protected synchronized Thread getThread() {
                return thread;
            }
        };

        this.setField("context", this.context, listener);

        listener.init();
        listener.process("");
        listener.process("exit");
    }
}
