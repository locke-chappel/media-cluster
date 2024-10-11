package io.github.lc.oss.mc.scheduler.app.thread;

import java.io.IOException;
import java.nio.file.FileSystem;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardWatchEventKinds;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.nio.file.spi.FileSystemProvider;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import io.github.lc.oss.mc.entity.Constants;
import io.github.lc.oss.mc.scheduler.AbstractMockTest;
import io.github.lc.oss.mc.scheduler.app.service.JobService;

public class IntakeThreadTest extends AbstractMockTest {
    private static class CallHelper {
        public boolean wasCalled = false;
    }

    @Test
    public void test_run_badMediaRoot() {
        IntakeThread thread = new IntakeThread();
        this.setField("root", null, thread);

        try {
            thread.run();
            Assertions.fail("Expected exception");
        } catch (RuntimeException ex) {
            Assertions.assertEquals("Error checking for media root at null" + Constants.FILE_SEPARATOR + "new",
                    ex.getMessage());
        }

        Assertions.assertFalse(thread.isRunning());
    }

    @Test
    public void test_run_badMediaRoot_v2() {
        IntakeThread thread = new IntakeThread() {
            @Override
            protected Path getMediaRoot() {
                return Paths.get(ProcessHandle.current().info().command().get());
            }
        };

        try {
            thread.run();
            Assertions.fail("Expected exception");
        } catch (RuntimeException ex) {
            Assertions.assertEquals("application.media.root is not a valid directory", ex.getMessage());
        }

        Assertions.assertFalse(thread.isRunning());
    }

    @Test
    public void test_run_fileSystem_error() {
        IntakeThread thread = new IntakeThread() {
            private Path p = Mockito.mock(Path.class);

            @Override
            protected Path getMediaRoot() {
                return this.p;
            }
        };

        FileSystem fs = Mockito.mock(FileSystem.class);
        FileSystemProvider fsp = Mockito.mock(FileSystemProvider.class);
        Map<String, Object> attrs = new HashMap<>();
        attrs.put("isDirectory", true);

        Mockito.when(thread.getMediaRoot().getFileSystem()).thenReturn(fs);
        Mockito.when(fs.provider()).thenReturn(fsp);
        try {
            Mockito.when(fsp.readAttributes(thread.getMediaRoot(), "basic:isDirectory", LinkOption.NOFOLLOW_LINKS))
                    .thenReturn(attrs);
            Mockito.when(fs.newWatchService()).thenThrow(new IOException("BOOM!"));
        } catch (IOException ex) {
            Assertions.fail("Unexpected exception");
        }

        try {
            thread.run();
            Assertions.fail("Expected exception");
        } catch (RuntimeException ex) {
            Assertions.assertEquals("Error watching filesystem", ex.getMessage());
            Assertions.assertEquals("BOOM!", ex.getCause().getMessage());
        }

        Assertions.assertFalse(thread.isRunning());
    }

    @Test
    public void test_run_watcher_error() {
        IntakeThread thread = new IntakeThread() {
            private Path p = Mockito.mock(Path.class);

            @Override
            protected Path getMediaRoot() {
                return this.p;
            }
        };

        FileSystem fs = Mockito.mock(FileSystem.class);
        FileSystemProvider fsp = Mockito.mock(FileSystemProvider.class);
        Map<String, Object> attrs = new HashMap<>();
        attrs.put("isDirectory", true);
        WatchService ws = Mockito.mock(WatchService.class);

        Mockito.when(thread.getMediaRoot().getFileSystem()).thenReturn(fs);
        Mockito.when(fs.provider()).thenReturn(fsp);
        try {
            Mockito.when(fsp.readAttributes(thread.getMediaRoot(), "basic:isDirectory", LinkOption.NOFOLLOW_LINKS))
                    .thenReturn(attrs);
            Mockito.when(fs.newWatchService()).thenReturn(ws);
            Mockito.when(ws.take()).thenThrow(new InterruptedException("WAKE UP!"));
        } catch (IOException | InterruptedException ex) {
            Assertions.fail("Unexpected exception");
        }

        try {
            thread.run();
            Assertions.fail("Expected exception");
        } catch (RuntimeException ex) {
            Assertions.assertEquals("Error watching filesystem", ex.getMessage());
            Assertions.assertEquals("WAKE UP!", ex.getCause().getMessage());
        }

        Assertions.assertFalse(thread.isRunning());
    }

    @Test
    public void test_run_shouldntRun() {
        IntakeThread thread = new IntakeThread() {
            private Path p = Mockito.mock(Path.class);

            @Override
            protected Path getMediaRoot() {
                return this.p;
            }
        };

        FileSystem fs = Mockito.mock(FileSystem.class);
        FileSystemProvider fsp = Mockito.mock(FileSystemProvider.class);
        Map<String, Object> attrs = new HashMap<>();
        attrs.put("isDirectory", true);

        Mockito.when(thread.getMediaRoot().getFileSystem()).thenReturn(fs);
        Mockito.when(fs.provider()).thenReturn(fsp);
        try {
            Mockito.when(fsp.readAttributes(thread.getMediaRoot(), "basic:isDirectory", LinkOption.NOFOLLOW_LINKS))
                    .thenReturn(attrs);
        } catch (IOException ex) {
            Assertions.fail("Unexpected exception");
        }

        thread.stop();
        Assertions.assertFalse((boolean) this.getField("shouldRun", thread));

        thread.run();

        Assertions.assertFalse(thread.isRunning());
    }

    @Test
    public void test_run_then_stop() {
        JobService jobService = Mockito.mock(JobService.class);

        final IntakeThread thread = new IntakeThread() {
            private Path p = Mockito.mock(Path.class);

            @Override
            protected Path getMediaRoot() {
                return this.p;
            }
        };
        this.setField("jobService", jobService, thread);

        FileSystem fs = Mockito.mock(FileSystem.class);
        FileSystemProvider fsp = Mockito.mock(FileSystemProvider.class);
        Map<String, Object> attrs = new HashMap<>();
        attrs.put("isDirectory", true);
        WatchService ws = Mockito.mock(WatchService.class);

        final Path newPath = Mockito.mock(Path.class);
        WatchKey wk = Mockito.mock(WatchKey.class);
        WatchEvent<Object> e1 = new WatchEvent<>() {
            @Override
            public Kind<Object> kind() {
                return StandardWatchEventKinds.OVERFLOW;
            }

            @Override
            public int count() {
                return 0;
            }

            @Override
            public Object context() {
                return null;
            }
        };
        WatchEvent<Path> e2 = new WatchEvent<>() {
            @Override
            public Kind<Path> kind() {
                return StandardWatchEventKinds.ENTRY_CREATE;
            }

            @Override
            public int count() {
                return 0;
            }

            @Override
            public Path context() {
                return newPath;
            }
        };

        final CallHelper jobHelper = new CallHelper();

        Mockito.when(thread.getMediaRoot().getFileSystem()).thenReturn(fs);
        Mockito.when(fs.provider()).thenReturn(fsp);
        Mockito.when(wk.pollEvents()).thenReturn(Arrays.asList(e1, e2));

        Mockito.doAnswer(new Answer<Path>() {
            @Override
            public Path answer(InvocationOnMock invocation) throws Throwable {
                thread.stop();
                return Path.of("/junit");
            }
        }).when(newPath).getFileName();

        Mockito.doAnswer(new Answer<Void>() {
            @Override
            public Void answer(InvocationOnMock invocation) throws Throwable {
                Assertions.assertFalse(jobHelper.wasCalled);
                jobHelper.wasCalled = true;
                return null;
            }
        }).when(jobService).newFile(ArgumentMatchers.notNull());

        try {
            Mockito.when(fsp.readAttributes(thread.getMediaRoot(), "basic:isDirectory", LinkOption.NOFOLLOW_LINKS))
                    .thenReturn(attrs);
            Mockito.when(fs.newWatchService()).thenReturn(ws);
            Mockito.when(ws.take()).thenReturn(wk);
        } catch (IOException | InterruptedException ex) {
            Assertions.fail("Unexpected exception");
        }

        Assertions.assertFalse(jobHelper.wasCalled);

        thread.run();

        Assertions.assertFalse(thread.isRunning());
        Assertions.assertTrue(jobHelper.wasCalled);
    }
}
