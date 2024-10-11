package io.github.lc.oss.mc.service;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import io.github.lc.oss.commons.l10n.L10N;
import io.github.lc.oss.commons.l10n.UserLocale;
import io.github.lc.oss.commons.serialization.Message;
import io.github.lc.oss.mc.AbstractMockTest;
import io.github.lc.oss.mc.api.Messages;
import io.github.lc.oss.mc.api.ServiceResponse;
import io.github.lc.oss.mc.entity.Constants;

public class FileServiceTest extends AbstractMockTest {
    private static String tempDir = null;

    @Mock
    private L10N l10n;
    @Mock
    private PathNormalizer pathNormalizer;
    @Mock
    private UserLocale userLocale;

    private FileService service;

    private String getTempDirPath() {
        if (FileServiceTest.tempDir == null) {
            FileServiceTest.tempDir = System.getProperty("java.io.tmpdir").replace("\\", "/");
            Assertions.assertTrue(Files.isDirectory(Paths.get(FileServiceTest.tempDir)),
                    String.format("'%s' is not a directory", FileServiceTest.tempDir));
            if (!FileServiceTest.tempDir.endsWith("/")) {
                FileServiceTest.tempDir += "/";
            }
        }
        return FileServiceTest.tempDir;
    }

    private String getTempRoot() {
        return this.getTempDirPath() + "FileServiceTest/";
    }

    @BeforeEach
    public void init() {
        this.service = new FileService() {
            @Override
            String getFileSeparator() {
                return "/";
            }
        };

        this.setField("l10n", this.l10n, this.service);
        this.setField("pathNormalizer", this.pathNormalizer, this.service);
        this.setField("userLocale", this.userLocale, this.service);
        this.setField("root", "/root/", this.service);
    }

    @AfterEach
    public void cleanup() {
        File f = new File(this.getTempDirPath() + "FileServiceTest");
        this.delete(f);
    }

    private void delete(File file) {
        File[] files = file.listFiles();
        if (files != null && files.length > 0) {
            for (File f : files) {
                this.delete(f);
            }
        }

        file.delete();
    }

    @Test
    public void test_defaultFileSeparator() {
        FileService service = new FileService();

        Assertions.assertSame(Constants.FILE_SEPARATOR, service.getFileSeparator());
    }

    @Test
    public void test_getProcessingDir() {
        Mockito.doAnswer(new Answer<String>() {
            @Override
            public String answer(InvocationOnMock invocation) throws Throwable {
                return invocation.getArgument(0);
            }
        }).when(this.pathNormalizer).dirOsAware(ArgumentMatchers.notNull());

        String result = this.service.getProcessingDir("junit");
        Assertions.assertEquals("/root/clusters/junit/processing", result);
    }

    @Test
    public void test_getTempDir() {
        Mockito.doAnswer(new Answer<String>() {
            @Override
            public String answer(InvocationOnMock invocation) throws Throwable {
                return invocation.getArgument(0);
            }
        }).when(this.pathNormalizer).dirOsAware(ArgumentMatchers.notNull());

        String result = this.service.getTempDir("junit");
        Assertions.assertEquals("/root/clusters/junit/temp", result);
    }

    @Test
    public void test_getDoneDir() {
        Mockito.doAnswer(new Answer<String>() {
            @Override
            public String answer(InvocationOnMock invocation) throws Throwable {
                return invocation.getArgument(0);
            }
        }).when(this.pathNormalizer).dirOsAware(ArgumentMatchers.notNull());

        String result = this.service.getDoneDir("junit");
        Assertions.assertEquals("/root/clusters/junit/done", result);
    }

    @Test
    public void test_getNameWithoutExt() {
        String result = this.service.getNameWithoutExt(null);
        Assertions.assertNull(result);

        result = this.service.getNameWithoutExt("");
        Assertions.assertEquals("", result);

        result = this.service.getNameWithoutExt(" \t \r \n \t ");
        Assertions.assertEquals(" \t \r \n \t ", result);

        result = this.service.getNameWithoutExt("no-ext");
        Assertions.assertEquals("no-ext", result);

        result = this.service.getNameWithoutExt("file.ext");
        Assertions.assertEquals("file", result);
    }

    @Test
    public void test_findNewFiles() {
        this.setField("root", "target/test-classes/root/", this.service);

        List<String> result = this.service.findNewFiles();
        Assertions.assertNotNull(result);
        Assertions.assertEquals(1, result.size());
        String file = result.iterator().next();
        Assertions.assertEquals("file.new", file);
    }

    @Test
    public void test_findDoneFiles() {
        this.setField("root", "target/test-classes/root/", this.service);

        List<String> result = this.service.findDoneFiles("file", "junit");
        Assertions.assertNotNull(result);
        Assertions.assertEquals(1, result.size());
        String file = result.iterator().next();
        Assertions.assertEquals("file.done", file);
    }

    @Test
    public void test_findProcessingFiles() {
        this.setField("root", "target/test-classes/root/", this.service);

        List<String> result = this.service.findProcessingFiles("file", "junit");
        Assertions.assertNotNull(result);
        Assertions.assertEquals(1, result.size());
        String file = result.iterator().next();
        Assertions.assertEquals("file.processing", file);
    }

    @Test
    public void test_findTemmpFiles() {
        this.setField("root", "target/test-classes/root/", this.service);

        List<String> result = this.service.findTempFiles("file", "junit");
        Assertions.assertNotNull(result);
        Assertions.assertEquals(1, result.size());
        String file = result.iterator().next();
        Assertions.assertEquals("file.temp", file);
    }

    @Test
    public void test_moveToProcessing_noSrc() {
        Mockito.doAnswer(new Answer<String>() {
            @Override
            public String answer(InvocationOnMock invocation) throws Throwable {
                return invocation.getArgument(0) + "/";
            }
        }).when(this.pathNormalizer).dirOsAware(ArgumentMatchers.notNull());

        ServiceResponse<?> result = this.service.moveToProcessing("file", "junit");
        Assertions.assertNotNull(result);
        Assertions.assertTrue(result.hasMessages(Messages.Application.NotFound));
        Assertions.assertNull(result.getEntity());
    }

    @Test
    public void test_moveToProcessing_alreadyExists() {
        this.setField("root", this.getTempRoot(), this.service);

        File newDir = new File(this.getTempRoot() + "new");
        newDir.mkdirs();
        File processingDir = new File(this.getTempRoot() + "clusters/junit/processing");
        processingDir.mkdirs();

        File srcFile = new File(this.getTempRoot() + "new/file.avi");
        File destFile = new File(this.getTempRoot() + "clusters/junit/processing/file.avi");
        try {
            srcFile.createNewFile();
            destFile.createNewFile();
        } catch (IOException ex) {
            Assertions.fail("Unexpected exception");
        }

        Mockito.doAnswer(new Answer<String>() {
            @Override
            public String answer(InvocationOnMock invocation) throws Throwable {
                return invocation.getArgument(0) + "/";
            }
        }).when(this.pathNormalizer).dirOsAware(ArgumentMatchers.notNull());

        Assertions.assertTrue(new File(this.getTempRoot() + "new/file.avi").exists());
        Assertions.assertTrue(new File(this.getTempRoot() + "clusters/junit/processing/file.avi").exists());

        ServiceResponse<?> result = this.service.moveToProcessing("file.avi", "junit");
        Assertions.assertNotNull(result);
        Assertions.assertTrue(result.hasMessages(Messages.Application.SourceAlradyExistsInCluster));
        Assertions.assertNull(result.getEntity());

        Assertions.assertTrue(new File(this.getTempRoot() + "new/file.avi").exists());
        Assertions.assertTrue(new File(this.getTempRoot() + "clusters/junit/processing/file.avi").exists());
    }

    @Test
    public void test_moveToProcessing() {
        this.setField("root", this.getTempRoot(), this.service);

        File newDir = new File(this.getTempRoot() + "new");
        newDir.mkdirs();
        File processingDir = new File(this.getTempRoot() + "clusters/junit/processing");
        processingDir.mkdirs();

        File srcFile = new File(this.getTempRoot() + "new/file.avi");
        try {
            srcFile.createNewFile();
        } catch (IOException ex) {
            Assertions.fail("Unexpected exception");
        }

        Mockito.doAnswer(new Answer<String>() {
            @Override
            public String answer(InvocationOnMock invocation) throws Throwable {
                return invocation.getArgument(0) + "/";
            }
        }).when(this.pathNormalizer).dirOsAware(ArgumentMatchers.notNull());

        Assertions.assertTrue(new File(this.getTempRoot() + "new/file.avi").exists());
        Assertions.assertFalse(new File(this.getTempRoot() + "clusters/junit/processing/file.avi").exists());

        ServiceResponse<?> result = this.service.moveToProcessing("file.avi", "junit");
        Assertions.assertNotNull(result);
        Assertions.assertFalse(result.hasMessages());
        Assertions.assertNull(result.getEntity());

        Assertions.assertFalse(new File(this.getTempRoot() + "new/file.avi").exists());
        Assertions.assertTrue(new File(this.getTempRoot() + "clusters/junit/processing/file.avi").exists());
    }

    @Test
    public void test_moveToProcessing_illegalSrc() {
        this.setField("root", this.getTempRoot(), this.service);

        File newDir = new File(this.getTempRoot() + "new");
        newDir.mkdirs();
        File processingDir = new File(this.getTempRoot() + "clusters/junit/processing");
        processingDir.mkdirs();

        Mockito.doAnswer(new Answer<String>() {
            @Override
            public String answer(InvocationOnMock invocation) throws Throwable {
                return invocation.getArgument(0) + "/";
            }
        }).when(this.pathNormalizer).dirOsAware(ArgumentMatchers.notNull());

        ServiceResponse<?> result = this.service.moveToProcessing("../file.avi", "junit");
        Assertions.assertNotNull(result);
        Assertions.assertTrue(result.hasMessages(Messages.Application.InvalidFilePath));
        Assertions.assertNull(result.getEntity());
    }

    @Test
    public void test_moveToNew() {
        this.setField("root", this.getTempRoot(), this.service);

        File newDir = new File(this.getTempRoot() + "new");
        newDir.mkdirs();
        File processingDir = new File(this.getTempRoot() + "clusters/junit/processing");
        processingDir.mkdirs();

        File srcFile = new File(this.getTempRoot() + "clusters/junit/processing/file.avi");
        try {
            srcFile.createNewFile();
        } catch (IOException ex) {
            Assertions.fail("Unexpected exception");
        }

        Mockito.doAnswer(new Answer<String>() {
            @Override
            public String answer(InvocationOnMock invocation) throws Throwable {
                return invocation.getArgument(0) + "/";
            }
        }).when(this.pathNormalizer).dirOsAware(ArgumentMatchers.notNull());

        Assertions.assertFalse(new File(this.getTempRoot() + "new/file.avi").exists());
        Assertions.assertTrue(new File(this.getTempRoot() + "clusters/junit/processing/file.avi").exists());

        ServiceResponse<?> result = this.service.moveToNew("file.avi", "junit");
        Assertions.assertNotNull(result);
        Assertions.assertFalse(result.hasMessages());
        Assertions.assertNull(result.getEntity());

        Assertions.assertTrue(new File(this.getTempRoot() + "new/file.avi").exists());
        Assertions.assertFalse(new File(this.getTempRoot() + "clusters/junit/processing/file.avi").exists());
    }

    @Test
    public void test_moveToNew_srcNotFound() {
        this.setField("root", this.getTempRoot(), this.service);

        File newDir = new File(this.getTempRoot() + "new");
        newDir.mkdirs();
        File processingDir = new File(this.getTempRoot() + "clusters/junit/processing");
        processingDir.mkdirs();

        Mockito.doAnswer(new Answer<String>() {
            @Override
            public String answer(InvocationOnMock invocation) throws Throwable {
                return invocation.getArgument(0) + "/";
            }
        }).when(this.pathNormalizer).dirOsAware(ArgumentMatchers.notNull());

        Assertions.assertFalse(new File(this.getTempRoot() + "new/file.avi").exists());

        ServiceResponse<?> result = this.service.moveToNew("file.avi", "junit");
        Assertions.assertNotNull(result);
        Assertions.assertTrue(result.hasMessages());
        Assertions.assertNull(result.getEntity());
        this.assertMessage(Messages.Application.NotFound, result.getMessages());
    }

    @Test
    public void test_moveToNew_illegalSrc() {
        this.setField("root", this.getTempRoot(), this.service);

        File newDir = new File(this.getTempRoot() + "new");
        newDir.mkdirs();
        File processingDir = new File(this.getTempRoot() + "clusters/junit/processing");
        processingDir.mkdirs();

        Mockito.doAnswer(new Answer<String>() {
            @Override
            public String answer(InvocationOnMock invocation) throws Throwable {
                return invocation.getArgument(0) + "/";
            }
        }).when(this.pathNormalizer).dirOsAware(ArgumentMatchers.notNull());

        ServiceResponse<?> result = this.service.moveToNew("../file.avi", "junit");
        Assertions.assertNotNull(result);
        Assertions.assertTrue(result.hasMessages(Messages.Application.InvalidFilePath));
        Assertions.assertNull(result.getEntity());
    }

    @Test
    public void test_moveToComplete() {
        this.setField("root", this.getTempRoot(), this.service);

        File completeDir = new File(this.getTempRoot() + "complete");
        completeDir.mkdirs();
        File doneDir = new File(this.getTempRoot() + "clusters/junit/done");
        doneDir.mkdirs();

        File srcFile = new File(this.getTempRoot() + "clusters/junit/done/file.mkv");
        try {
            srcFile.createNewFile();
        } catch (IOException ex) {
            Assertions.fail("Unexpected exception");
        }

        Mockito.doAnswer(new Answer<String>() {
            @Override
            public String answer(InvocationOnMock invocation) throws Throwable {
                return invocation.getArgument(0) + "/";
            }
        }).when(this.pathNormalizer).dirOsAware(ArgumentMatchers.notNull());

        Assertions.assertFalse(new File(this.getTempRoot() + "complete/file.mkv").exists());
        Assertions.assertTrue(new File(this.getTempRoot() + "clusters/junit/done/file.mkv").exists());

        ServiceResponse<?> result = this.service.moveToComplete("file.ts", "mkv", "junit");
        Assertions.assertNotNull(result);
        Assertions.assertFalse(result.hasMessages());
        Assertions.assertNull(result.getEntity());

        Assertions.assertTrue(new File(this.getTempRoot() + "complete/file.mkv").exists());
        Assertions.assertFalse(new File(this.getTempRoot() + "clusters/junit/done/file.mkv").exists());
    }

    @Test
    public void test_moveToComplete_srcNotFound() {
        this.setField("root", this.getTempRoot(), this.service);

        File completeDir = new File(this.getTempRoot() + "complete");
        completeDir.mkdirs();
        File doneDir = new File(this.getTempRoot() + "clusters/junit/done");
        doneDir.mkdirs();

        Mockito.doAnswer(new Answer<String>() {
            @Override
            public String answer(InvocationOnMock invocation) throws Throwable {
                return invocation.getArgument(0) + "/";
            }
        }).when(this.pathNormalizer).dirOsAware(ArgumentMatchers.notNull());

        Assertions.assertFalse(new File(this.getTempRoot() + "complete/file.mkv").exists());
        Assertions.assertFalse(new File(this.getTempRoot() + "clusters/junit/done/file.mkv").exists());

        ServiceResponse<?> result = this.service.moveToComplete("file.ts", "mkv", "junit");
        Assertions.assertNotNull(result);
        Assertions.assertTrue(result.hasMessages());
        Assertions.assertNull(result.getEntity());
        this.assertMessage(Messages.Application.NotFound, result.getMessages());
    }

    @Test
    public void test_moveToComplete_illegalSrc() {
        this.setField("root", this.getTempRoot(), this.service);

        File completeDir = new File(this.getTempRoot() + "complete");
        completeDir.mkdirs();
        File doneDir = new File(this.getTempRoot() + "clusters/junit/done");
        doneDir.mkdirs();

        Mockito.doAnswer(new Answer<String>() {
            @Override
            public String answer(InvocationOnMock invocation) throws Throwable {
                return invocation.getArgument(0) + "/";
            }
        }).when(this.pathNormalizer).dirOsAware(ArgumentMatchers.notNull());

        ServiceResponse<?> result = this.service.moveToComplete("../file.ts", "mkv", "junit");
        Assertions.assertNotNull(result);
        Assertions.assertTrue(result.hasMessages(Messages.Application.InvalidFilePath));
        Assertions.assertNull(result.getEntity());
    }

    @Test
    public void test_cleanProcessing() {
        this.setField("root", this.getTempRoot(), this.service);

        File processingDir = new File(this.getTempRoot() + "clusters/junit/processing");
        processingDir.mkdirs();

        File srcFile = new File(this.getTempRoot() + "clusters/junit/processing/file.m2ts");
        try {
            srcFile.createNewFile();
        } catch (IOException ex) {
            Assertions.fail("Unexpected exception");
        }

        Mockito.doAnswer(new Answer<String>() {
            @Override
            public String answer(InvocationOnMock invocation) throws Throwable {
                return invocation.getArgument(0) + "/";
            }
        }).when(this.pathNormalizer).dirOsAware(ArgumentMatchers.notNull());

        Assertions.assertTrue(new File(this.getTempRoot() + "clusters/junit/processing/file.m2ts").exists());

        ServiceResponse<?> result = this.service.cleanProcessing("file", "junit");
        Assertions.assertNotNull(result);
        Assertions.assertFalse(result.hasMessages());
        Assertions.assertNull(result.getEntity());

        Assertions.assertFalse(new File(this.getTempRoot() + "clusters/junit/processing/file.m2ts").exists());
    }

    @Test
    public void test_cleanProcessing_noFiles() {
        this.setField("root", this.getTempRoot(), this.service);

        File processingDir = new File(this.getTempRoot() + "clusters/junit/processing");
        processingDir.mkdirs();

        ServiceResponse<?> result = this.service.cleanProcessing("file", "junit");
        Assertions.assertNotNull(result);
        Assertions.assertFalse(result.hasMessages());
        Assertions.assertNull(result.getEntity());
    }

    @Test
    public void test_cleanTemp() {
        this.setField("root", this.getTempRoot(), this.service);

        File processingDir = new File(this.getTempRoot() + "clusters/junit/temp");
        processingDir.mkdirs();

        File srcFile = new File(this.getTempRoot() + "clusters/junit/temp/file.m2ts");
        try {
            srcFile.createNewFile();
        } catch (IOException ex) {
            Assertions.fail("Unexpected exception");
        }

        Mockito.doAnswer(new Answer<String>() {
            @Override
            public String answer(InvocationOnMock invocation) throws Throwable {
                return invocation.getArgument(0) + "/";
            }
        }).when(this.pathNormalizer).dirOsAware(ArgumentMatchers.notNull());

        Assertions.assertTrue(new File(this.getTempRoot() + "clusters/junit/temp/file.m2ts").exists());

        ServiceResponse<?> result = this.service.cleanTemp("file.", "junit");
        Assertions.assertNotNull(result);
        Assertions.assertFalse(result.hasMessages());
        Assertions.assertNull(result.getEntity());
    }

    @Test
    public void test_cleanTemp_noFiles() {
        this.setField("root", this.getTempRoot(), this.service);

        File processingDir = new File(this.getTempRoot() + "clusters/junit/temp");
        processingDir.mkdirs();

        ServiceResponse<?> result = this.service.cleanTemp("file.", "junit");
        Assertions.assertNotNull(result);
        Assertions.assertFalse(result.hasMessages());
        Assertions.assertNull(result.getEntity());
    }

    @Test
    public void test_toPaths_null_via_findNewFiles_invalidRoot() {
        this.setField("root", null, this.service);

        List<String> result = this.service.findNewFiles();
        Assertions.assertNotNull(result);
        Assertions.assertTrue(result.isEmpty());
    }

    private void assertMessage(Message expected, Collection<Message> actual) {
        Assertions.assertNotNull(actual);
        Assertions.assertEquals(1, actual.size());
        Message message = actual.iterator().next();
        Assertions.assertNotNull(message);
        Assertions.assertEquals(expected.getCategory(), message.getCategory());
        Assertions.assertEquals(expected.getSeverity(), message.getSeverity());
        Assertions.assertEquals(expected.getNumber(), message.getNumber());
    }
}
