package io.github.lc.oss.mc.worker.app.service;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.slf4j.Logger;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import io.github.lc.oss.commons.api.services.JsonService;
import io.github.lc.oss.commons.serialization.Message;
import io.github.lc.oss.commons.util.PathNormalizer;
import io.github.lc.oss.commons.web.services.HttpService;
import io.github.lc.oss.mc.api.ApiObjectResponse;
import io.github.lc.oss.mc.api.ApiResponse;
import io.github.lc.oss.mc.api.Job;
import io.github.lc.oss.mc.api.JobResult;
import io.github.lc.oss.mc.api.JobTypes;
import io.github.lc.oss.mc.api.Messages;
import io.github.lc.oss.mc.api.Profile;
import io.github.lc.oss.mc.service.FileService;
import io.github.lc.oss.mc.worker.AbstractMockTest;

public class FFMPEGServiceTest extends AbstractMockTest {
    private static class CallCounter {
        public int count = 0;
    }

    private static interface ExecHelper {
        void assertCommand(String... command);
    }

    @Mock
    private Environment env;
    @Mock
    private FileService fileService;
    @Mock
    private HttpService httpService;
    @Mock
    private JsonService jsonService;
    @Mock
    private PathNormalizer pathNormalizer;

    private Process ffmpegProcess;

    private FFMPEGService service = new FFMPEGService() {
        public Process ffmpegProcess;
        public ExecHelper execHelper;

        @Override
        protected Process exec(String... command) {
            if (this.execHelper != null) {
                this.execHelper.assertCommand(command);
            }
            return this.ffmpegProcess;
        };
    };

    @BeforeEach
    public void setup() {
        this.ffmpegProcess = Mockito.mock(Process.class);

        this.setField("currentJob", null, this.service);
        this.setField("currentProcess", null, this.service);
        this.setField("retryCount", 1, this.service);
        this.setField("ffmpegPath", "ffmpeg", this.service);
        this.setField("ffprobePath", "ffprobe", this.service);

        this.setField("config", this.getConfig(), this.service);
        this.setField("env", this.env, this.service);
        this.setField("fileService", this.fileService, this.service);
        this.setField("httpService", this.httpService, this.service);
        this.setField("jsonService", this.jsonService, this.service);

        this.setField("ffmpegProcess", this.ffmpegProcess, this.service);
        this.setField("execHelper", null, this.service);
    }

    @Test
    public void test_onExit_blankUrl_error() {
        Mockito.when(this.getConfig().getSchedulerUrl()).thenReturn(" \t \r \n \t ");
        Mockito.when(this.env.getProperty("integrationtest", Boolean.class, Boolean.FALSE)).thenReturn(false);

        try {
            this.service.onExit();
            Assertions.fail("Expected exception");
        } catch (RuntimeException ex) {
            Assertions.assertEquals("Invalid configuration - scheduler URL cannot be blank", ex.getMessage());
        }
    }

    @Test
    public void test_onExit_callScheduler_gatewayTimeout() {
        Job job = this.fac().job();
        JobResult jobResult = new JobResult();
        this.setField("currentJob", job, this.service);
        this.setField("currentResult", jobResult, this.service);

        @SuppressWarnings("unchecked")
        ResponseEntity<ApiResponse> apiResponse = Mockito.mock(ResponseEntity.class);

        Mockito.when(this.jsonService.to(ArgumentMatchers.notNull())).thenReturn("{}");
        Mockito.when(this.getConfig().getSchedulerUrl()).thenReturn("https://localhost:8081");
        Mockito.when(this.getConfig().getId()).thenReturn("junit");
        this.expectPrivateKey();
        Mockito.when(this.httpService.call(ArgumentMatchers.eq(HttpMethod.POST),
                ArgumentMatchers.eq("https://localhost:8081/api/v1/jobs/complete"), ArgumentMatchers.notNull(),
                ArgumentMatchers.eq(ApiResponse.class), ArgumentMatchers.notNull())).thenReturn(apiResponse);
        Mockito.when(apiResponse.getStatusCode()).thenReturn(HttpStatus.GATEWAY_TIMEOUT);

        this.service.onExit();
    }

    @Test
    public void test_onExit_callScheduler_error_noMessages() {
        Job job = this.fac().job();
        JobResult jobResult = new JobResult();
        this.setField("currentJob", job, this.service);
        this.setField("currentResult", jobResult, this.service);

        @SuppressWarnings("unchecked")
        ResponseEntity<ApiResponse> apiResponse = Mockito.mock(ResponseEntity.class);

        Mockito.when(this.jsonService.to(ArgumentMatchers.notNull())).thenReturn("{}");
        Mockito.when(this.getConfig().getSchedulerUrl()).thenReturn("https://localhost:8081");
        Mockito.when(this.getConfig().getId()).thenReturn("junit");
        this.expectPrivateKey();
        Mockito.when(this.httpService.call(ArgumentMatchers.eq(HttpMethod.POST),
                ArgumentMatchers.eq("https://localhost:8081/api/v1/jobs/complete"), ArgumentMatchers.notNull(),
                ArgumentMatchers.eq(ApiResponse.class), ArgumentMatchers.notNull())).thenReturn(apiResponse);
        Mockito.when(apiResponse.getStatusCode()).thenReturn(HttpStatus.UNPROCESSABLE_ENTITY);
        Mockito.when(apiResponse.getBody()).thenReturn(null);

        this.service.onExit();
    }

    @Test
    public void test_onExit_callScheduler_error_nullMessages() {
        Job job = this.fac().job();
        JobResult jobResult = new JobResult();
        this.setField("currentJob", job, this.service);
        this.setField("currentResult", jobResult, this.service);

        @SuppressWarnings("unchecked")
        ResponseEntity<ApiResponse> apiResponse = Mockito.mock(ResponseEntity.class);
        ApiResponse response = new ApiResponse() {
            @Override
            public Collection<Messages> getMessages() {
                return null;
            }
        };

        Mockito.when(this.jsonService.to(ArgumentMatchers.notNull())).thenReturn("{}");
        Mockito.when(this.getConfig().getSchedulerUrl()).thenReturn("https://localhost:8081");
        Mockito.when(this.getConfig().getId()).thenReturn("junit");
        this.expectPrivateKey();
        Mockito.when(this.httpService.call(ArgumentMatchers.eq(HttpMethod.POST),
                ArgumentMatchers.eq("https://localhost:8081/api/v1/jobs/complete"), ArgumentMatchers.notNull(),
                ArgumentMatchers.eq(ApiResponse.class), ArgumentMatchers.notNull())).thenReturn(apiResponse);
        Mockito.when(apiResponse.getStatusCode()).thenReturn(HttpStatus.UNPROCESSABLE_ENTITY);
        Mockito.when(apiResponse.getBody()).thenReturn(response);

        this.service.onExit();
    }

    @Test
    public void test_onExit_callScheduler_error_emptyMessages() {
        Job job = this.fac().job();
        JobResult jobResult = new JobResult();
        this.setField("currentJob", job, this.service);
        this.setField("currentResult", jobResult, this.service);

        @SuppressWarnings("unchecked")
        ResponseEntity<ApiResponse> apiResponse = Mockito.mock(ResponseEntity.class);
        ApiObjectResponse<JobResult> response = new ApiObjectResponse<JobResult>() {
            @Override
            public JobResult getBody() {
                return null;
            }

            @Override
            public Collection<Messages> getMessages() {
                return new ArrayList<>();
            }
        };

        Mockito.when(this.jsonService.to(ArgumentMatchers.notNull())).thenReturn("{}");
        Mockito.when(this.getConfig().getSchedulerUrl()).thenReturn("https://localhost:8081");
        Mockito.when(this.getConfig().getId()).thenReturn("junit");
        this.expectPrivateKey();
        Mockito.when(this.httpService.call(ArgumentMatchers.eq(HttpMethod.POST),
                ArgumentMatchers.eq("https://localhost:8081/api/v1/jobs/complete"), ArgumentMatchers.notNull(),
                ArgumentMatchers.eq(ApiResponse.class), ArgumentMatchers.notNull())).thenReturn(apiResponse);
        Mockito.when(apiResponse.getStatusCode()).thenReturn(HttpStatus.UNPROCESSABLE_ENTITY);
        Mockito.when(apiResponse.getBody()).thenReturn(response);

        this.service.onExit();
    }

    @Test
    public void test_onExit_callScheduler_error_withMessages() {
        Job job = this.fac().job();
        JobResult jobResult = new JobResult();
        this.setField("currentJob", job, this.service);
        this.setField("currentResult", jobResult, this.service);

        @SuppressWarnings("unchecked")
        ResponseEntity<ApiResponse> apiResponse = Mockito.mock(ResponseEntity.class);
        ApiResponse response = new ApiResponse() {
            @Override
            public Collection<Messages> getMessages() {
                return Arrays.asList(new Messages(Messages.Authentication.InvalidSignature));
            }
        };

        Mockito.when(this.jsonService.to(ArgumentMatchers.notNull())).thenReturn("{}");
        Mockito.when(this.getConfig().getSchedulerUrl()).thenReturn("https://localhost:8081");
        Mockito.when(this.getConfig().getId()).thenReturn("junit");
        this.expectPrivateKey();
        Mockito.when(this.httpService.call(ArgumentMatchers.eq(HttpMethod.POST),
                ArgumentMatchers.eq("https://localhost:8081/api/v1/jobs/complete"), ArgumentMatchers.notNull(),
                ArgumentMatchers.eq(ApiResponse.class), ArgumentMatchers.notNull())).thenReturn(apiResponse);
        Mockito.when(apiResponse.getStatusCode()).thenReturn(HttpStatus.UNPROCESSABLE_ENTITY);
        Mockito.when(apiResponse.getBody()).thenReturn(response);

        this.service.onExit();
    }

    @Test
    public void test_onExit_callScheduler_tooManyRetries() {
        FFMPEGService service = new FFMPEGService() {
            private Logger logger = Mockito.mock(Logger.class);

            @Override
            protected Logger getLogger() {
                return this.logger;
            }
        };
        this.setField("config", this.getConfig(), service);
        this.setField("retryCount", 3, service);

        Mockito.when(this.getConfig().getSchedulerUrl()).thenReturn("https://localhost:8081");
        Mockito.when(this.getConfig().getId()).thenThrow(new RuntimeException("Boom!"));

        final CallCounter loggerCount = new CallCounter();
        Mockito.doAnswer(new Answer<Void>() {
            @Override
            public Void answer(InvocationOnMock invocation) throws Throwable {
                loggerCount.count++;
                return null;
            }
        }).when(service.getLogger()).error(ArgumentMatchers.notNull(), (RuntimeException) ArgumentMatchers.notNull());

        Assertions.assertEquals(0, loggerCount.count);
        service.onExit();
        Assertions.assertEquals(3, loggerCount.count);
    }

    @Test
    public void test_onExit_callScheduler() {
        Job job = this.fac().job();
        JobResult jobResult = new JobResult();
        this.setField("currentJob", job, this.service);
        this.setField("currentResult", jobResult, this.service);

        @SuppressWarnings("unchecked")
        ResponseEntity<ApiResponse> apiResponse = Mockito.mock(ResponseEntity.class);

        Mockito.when(this.jsonService.to(ArgumentMatchers.notNull())).thenReturn("{}");
        Mockito.when(this.getConfig().getSchedulerUrl()).thenReturn("https://localhost:8081");
        Mockito.when(this.getConfig().getId()).thenReturn("junit");
        this.expectPrivateKey();
        Mockito.when(this.httpService.call( //
                ArgumentMatchers.eq(HttpMethod.POST), //
                ArgumentMatchers.eq("https://localhost:8081/api/v1/jobs/complete"), //
                ArgumentMatchers.notNull(), //
                ArgumentMatchers.eq(ApiResponse.class), //
                ArgumentMatchers.notNull())). //
                thenReturn(apiResponse);
        Mockito.when(apiResponse.getStatusCode()).thenReturn(HttpStatus.NO_CONTENT);

        this.service.onExit();
    }

    @Test
    public void test_getNullFile() {
        FFMPEGService win = new FFMPEGService() {
            @Override
            String getFileSeparator() {
                return "\\";
            }
        };

        FFMPEGService nix = new FFMPEGService() {
            @Override
            String getFileSeparator() {
                return "/";
            }
        };

        String nullW = win.getNullFile();
        String nullN = nix.getNullFile();
        Assertions.assertNotEquals(nullW, nullN);

        Assertions.assertEquals("NUL", nullW);
        Assertions.assertSame(nullW, win.getNullFile());

        Assertions.assertEquals("/dev/null", nullN);
        Assertions.assertSame(nullN, nix.getNullFile());
    }

    @Test
    public void test_getFFMPEGCommand_filePath_win() {
        FFMPEGService service = new FFMPEGService() {
            @Override
            String getFileSeparator() {
                return "\\";
            }
        };
        this.setField("ffmpegPath", "C:\\Program\\ Files/ffmpeg/ffmpeg.exe", service);

        String[] result = service.getFFMPEGCommand();
        Assertions.assertNotNull(result);
        Assertions.assertSame(result, service.getFFMPEGCommand());
        Assertions.assertEquals(4, result.length);
        Assertions.assertEquals("C:\\Program Files\\ffmpeg\\ffmpeg.exe", result[0]);
        Assertions.assertEquals("-n", result[1]);
        Assertions.assertEquals("-loglevel", result[2]);
        Assertions.assertEquals("error", result[3]);
    }

    @Test
    public void test_getFFMPEGCommand_filePath_win_v2() {
        FFMPEGService service = new FFMPEGService() {
            @Override
            String getFileSeparator() {
                return "\\";
            }
        };
        this.setField("ffmpegPath", "Program\\ Files/ffmpeg/ffmpeg.exe", service);

        String[] result = service.getFFMPEGCommand();
        Assertions.assertNotNull(result);
        Assertions.assertSame(result, service.getFFMPEGCommand());
        Assertions.assertEquals(4, result.length);
        Assertions.assertEquals("Program Files/ffmpeg/ffmpeg.exe", result[0]);
        Assertions.assertEquals("-n", result[1]);
        Assertions.assertEquals("-loglevel", result[2]);
        Assertions.assertEquals("error", result[3]);
    }

    @Test
    public void test_getFFMPEGCommand_filePath_nix() {
        FFMPEGService service = new FFMPEGService() {
            @Override
            String getFileSeparator() {
                return "/";
            }
        };
        this.setField("ffmpegPath", "/usr/bin/ffmpeg", service);

        String[] result = service.getFFMPEGCommand();
        Assertions.assertNotNull(result);
        Assertions.assertSame(result, service.getFFMPEGCommand());
        Assertions.assertEquals(4, result.length);
        Assertions.assertEquals("/usr/bin/ffmpeg", result[0]);
        Assertions.assertEquals("-n", result[1]);
        Assertions.assertEquals("-loglevel", result[2]);
        Assertions.assertEquals("error", result[3]);
    }

    @Test
    public void test_getFFProbeCommand_filePath_win() {
        FFMPEGService service = new FFMPEGService() {
            @Override
            String getFileSeparator() {
                return "\\";
            }
        };
        this.setField("ffprobePath", "C:\\Program\\ Files/ffmpeg/ffprobe.exe", service);

        String[] result = service.getFFProbeCommand();
        Assertions.assertNotNull(result);
        Assertions.assertSame(result, service.getFFProbeCommand());
        Assertions.assertEquals(1, result.length);
    }

    @Test
    public void test_getFFProbeCommand_filePath_win_v2() {
        FFMPEGService service = new FFMPEGService() {
            @Override
            String getFileSeparator() {
                return "\\";
            }
        };
        this.setField("ffprobePath", "Program\\ Files/ffmpeg/ffprobe.exe", service);

        String[] result = service.getFFProbeCommand();
        Assertions.assertNotNull(result);
        Assertions.assertSame(result, service.getFFProbeCommand());
        Assertions.assertEquals(1, result.length);
    }

    @Test
    public void test_getFFProbeCommand_filePath_nix() {
        FFMPEGService service = new FFMPEGService() {
            @Override
            String getFileSeparator() {
                return "/";
            }
        };
        this.setField("ffprobePath", "/usr/bin/ffprobe", service);

        String[] result = service.getFFProbeCommand();
        Assertions.assertNotNull(result);
        Assertions.assertSame(result, service.getFFProbeCommand());
        Assertions.assertEquals(1, result.length);
        Assertions.assertEquals("/usr/bin/ffprobe", result[0]);
    }

    @Test
    public void test_process_busy_hasProcess() {
        Job job = this.fac().job();

        this.setField("currentProcess", Mockito.mock(Process.class), this.service);

        Set<Message> result = this.service.process(job);
        Assertions.assertNotNull(result);
        Assertions.assertEquals(1, result.size());
        Assertions.assertEquals(Messages.Application.AlreadyProcessingJob, result.iterator().next());

        Assertions.assertNull(this.getField("currentJob", this.service));
        Assertions.assertNull(this.getField("currentResult", this.service));
    }

    @Test
    public void test_process_busy_hasJob() {
        Job job = this.fac().job();

        this.setField("currentJob", new Job(), this.service);

        Set<Message> result = this.service.process(job);
        Assertions.assertNotNull(result);
        Assertions.assertEquals(1, result.size());
        Assertions.assertEquals(Messages.Application.AlreadyProcessingJob, result.iterator().next());

        Assertions.assertNotNull(this.getField("currentJob", this.service));
        Assertions.assertNull(this.getField("currentResult", this.service));
    }

    @Test
    public void test_process_execError_noProcess() {
        Job job = this.fac().job();
        job.setType(JobTypes.Audio);

        Profile profile = this.fac().profile();

        Mockito.when(this.fileService.getNameWithoutExt(job.getSource())).thenReturn("sleep");
        Mockito.when(this.jsonService.from(job.getProfile(), Profile.class)).thenReturn(profile);
        Mockito.when(this.getConfig().getClusterName()).thenReturn("junit");

        Mockito.doAnswer(new Answer<String>() {
            @Override
            public String answer(InvocationOnMock invocation) throws Throwable {
                return "/media/clusters/" + invocation.getArgument(0) + "/processing/";
            }
        }).when(this.fileService).getProcessingDir(ArgumentMatchers.notNull());
        Mockito.doAnswer(new Answer<String>() {
            @Override
            public String answer(InvocationOnMock invocation) throws Throwable {
                return "/media/clusters/" + invocation.getArgument(0) + "/temp/";
            }
        }).when(this.fileService).getTempDir(ArgumentMatchers.notNull());

        ExecHelper helper = new ExecHelper() {
            String[] expected = new String[] { //
                    "ffmpeg", //
                    "-n", //
                    "-loglevel", "error", //
                    "-i", "/media/clusters/junit/processing/sleep.avi", //
                    "-c:a", "copy", //
                    "-vn", //
                    "/media/clusters/junit/temp/sleep-Audio.ts" };

            @Override
            public void assertCommand(String... command) {
                Assertions.assertEquals(this.expected.length, command.length);
                for (int i = 0; i < this.expected.length; i++) {
                    Assertions.assertEquals(this.expected[i], command[i]);
                }
            }
        };

        this.setField("execHelper", helper, this.service);

        // emulate failed process execution
        this.setField("ffmpegProcess", null, this.service);

        Set<Message> result = this.service.process(job);
        Assertions.assertNotNull(result);
        Assertions.assertEquals(1, result.size());
        Assertions.assertEquals(Messages.Application.FailedToStartJob, result.iterator().next());

        Assertions.assertNull(this.getField("currentJob", this.service));
        Assertions.assertNull(this.getField("currentResult", this.service));
    }

    @Test
    public void test_process_scan_frameCountError() {
        Job job = this.fac().job();
        job.setType(JobTypes.Scan);

        Profile profile = this.fac().profile();
        @SuppressWarnings("unchecked")
        CompletableFuture<Process> onExit = Mockito.mock(CompletableFuture.class);

        InputStream ffmpegErrorStream = new ByteArrayInputStream("done".getBytes(StandardCharsets.UTF_8));

        Mockito.when(this.fileService.getNameWithoutExt(job.getSource())).thenReturn("sleep");
        Mockito.when(this.jsonService.from(job.getProfile(), Profile.class)).thenReturn(profile);
        Mockito.when(this.getConfig().getClusterName()).thenReturn("junit");
        Mockito.when(this.ffmpegProcess.getInputStream()).thenReturn(ffmpegErrorStream);
        Mockito.when(this.ffmpegProcess.onExit()).thenReturn(onExit);

        Mockito.doAnswer(new Answer<String>() {
            @Override
            public String answer(InvocationOnMock invocation) throws Throwable {
                return "/media/clusters/" + invocation.getArgument(0) + "/processing/";
            }
        }).when(this.fileService).getProcessingDir(ArgumentMatchers.notNull());

        ExecHelper helper = new ExecHelper() {
            String[] expected = new String[] { //
                    "ffprobe", //
                    "-v", "error", //
                    "-select_streams", "v:0", //
                    "-count_frames", //
                    "-show_entries", "stream=nb_read_frames", //
                    "-of", "csv=p=0", //
                    "/media/clusters/junit/processing/sleep.avi" };

            @Override
            public void assertCommand(String... command) {
                Assertions.assertEquals(this.expected.length, command.length);
                for (int i = 0; i < this.expected.length; i++) {
                    Assertions.assertEquals(this.expected[i], command[i]);
                }
            }
        };

        this.setField("execHelper", helper, this.service);

        Set<Message> result = this.service.process(job);
        Assertions.assertNotNull(result);
        Assertions.assertTrue(result.isEmpty());
    }

    @Test
    public void test_process_scan_frameCountError_v2() {
        Job job = this.fac().job();
        job.setType(JobTypes.Scan);

        Profile profile = this.fac().profile();
        @SuppressWarnings("unchecked")
        CompletableFuture<Process> onExit = Mockito.mock(CompletableFuture.class);

        InputStream ffmpegErrorStream = new ByteArrayInputStream("frame=".getBytes(StandardCharsets.UTF_8));

        Mockito.when(this.fileService.getNameWithoutExt(job.getSource())).thenReturn("sleep");
        Mockito.when(this.jsonService.from(job.getProfile(), Profile.class)).thenReturn(profile);
        Mockito.when(this.getConfig().getClusterName()).thenReturn("junit");
        Mockito.when(this.ffmpegProcess.getInputStream()).thenReturn(ffmpegErrorStream);
        Mockito.when(this.ffmpegProcess.onExit()).thenReturn(onExit);

        Mockito.doAnswer(new Answer<String>() {
            @Override
            public String answer(InvocationOnMock invocation) throws Throwable {
                return "/media/clusters/" + invocation.getArgument(0) + "/processing/";
            }
        }).when(this.fileService).getProcessingDir(ArgumentMatchers.notNull());

        ExecHelper helper = new ExecHelper() {
            String[] expected = new String[] { //
                    "ffprobe", //
                    "-v", "error", //
                    "-select_streams", "v:0", //
                    "-count_frames", //
                    "-show_entries", "stream=nb_read_frames", //
                    "-of", "csv=p=0", //
                    "/media/clusters/junit/processing/sleep.avi" };

            @Override
            public void assertCommand(String... command) {
                Assertions.assertEquals(this.expected.length, command.length);
                for (int i = 0; i < this.expected.length; i++) {
                    Assertions.assertEquals(this.expected[i], command[i]);
                }
            }
        };

        this.setField("execHelper", helper, this.service);

        Set<Message> result = this.service.process(job);
        Assertions.assertNotNull(result);
        Assertions.assertTrue(result.isEmpty());
    }

    @Test
    public void test_process_scan_frameCountError_v3() {
        Job job = this.fac().job();
        job.setType(JobTypes.Scan);

        Profile profile = this.fac().profile();
        @SuppressWarnings("unchecked")
        CompletableFuture<Process> onExit = Mockito.mock(CompletableFuture.class);

        InputStream ffmpegErrorStream = new ByteArrayInputStream("".getBytes(StandardCharsets.UTF_8));

        Mockito.when(this.fileService.getNameWithoutExt(job.getSource())).thenReturn("sleep");
        Mockito.when(this.jsonService.from(job.getProfile(), Profile.class)).thenReturn(profile);
        Mockito.when(this.getConfig().getClusterName()).thenReturn("junit");
        Mockito.when(this.ffmpegProcess.getInputStream()).thenReturn(ffmpegErrorStream);
        Mockito.when(this.ffmpegProcess.onExit()).thenReturn(onExit);

        Mockito.doAnswer(new Answer<String>() {
            @Override
            public String answer(InvocationOnMock invocation) throws Throwable {
                return "/media/clusters/" + invocation.getArgument(0) + "/processing/";
            }
        }).when(this.fileService).getProcessingDir(ArgumentMatchers.notNull());

        ExecHelper helper = new ExecHelper() {
            String[] expected = new String[] { //
                    "ffprobe", //
                    "-v", "error", //
                    "-select_streams", "v:0", //
                    "-count_frames", //
                    "-show_entries", "stream=nb_read_frames", //
                    "-of", "csv=p=0", //
                    "/media/clusters/junit/processing/sleep.avi" };

            @Override
            public void assertCommand(String... command) {
                Assertions.assertEquals(this.expected.length, command.length);
                for (int i = 0; i < this.expected.length; i++) {
                    Assertions.assertEquals(this.expected[i], command[i]);
                }
            }
        };

        this.setField("execHelper", helper, this.service);

        Set<Message> result = this.service.process(job);
        Assertions.assertNotNull(result);
        Assertions.assertTrue(result.isEmpty());
    }

    @Test
    public void test_process_scan_noCommonArgs() {
        Job job = this.fac().job();
        job.setType(JobTypes.Scan);

        Profile profile = this.fac().profile();
        @SuppressWarnings("unchecked")
        CompletableFuture<Process> onExit = Mockito.mock(CompletableFuture.class);

        InputStream ffmpegErrorStream = new ByteArrayInputStream("99".getBytes(StandardCharsets.UTF_8));

        Mockito.when(this.fileService.getNameWithoutExt(job.getSource())).thenReturn("sleep");
        Mockito.when(this.jsonService.from(job.getProfile(), Profile.class)).thenReturn(profile);
        Mockito.when(this.getConfig().getClusterName()).thenReturn("junit");
        Mockito.when(this.ffmpegProcess.getInputStream()).thenReturn(ffmpegErrorStream);
        Mockito.when(this.ffmpegProcess.onExit()).thenReturn(onExit);

        Mockito.doAnswer(new Answer<String>() {
            @Override
            public String answer(InvocationOnMock invocation) throws Throwable {
                return "/media/clusters/" + invocation.getArgument(0) + "/processing/";
            }
        }).when(this.fileService).getProcessingDir(ArgumentMatchers.notNull());

        ExecHelper helper = new ExecHelper() {
            String[] expected = new String[] { //
                    "ffprobe", //
                    "-v", "error", //
                    "-select_streams", "v:0", //
                    "-count_frames", //
                    "-show_entries", "stream=nb_read_frames", //
                    "-of", "csv=p=0", //
                    "/media/clusters/junit/processing/sleep.avi" };

            @Override
            public void assertCommand(String... command) {
                Assertions.assertEquals(this.expected.length, command.length);
                for (int i = 0; i < this.expected.length; i++) {
                    Assertions.assertEquals(this.expected[i], command[i]);
                }
            }
        };

        this.setField("execHelper", helper, this.service);

        Set<Message> result = this.service.process(job);
        Assertions.assertNotNull(result);
        Assertions.assertTrue(result.isEmpty());
    }

    @Test
    public void test_process_scan_withCommonArgs() {
        Job job = this.fac().job();
        job.setType(JobTypes.Scan);

        Profile profile = this.fac().profile();
        profile.setCommonArgs(Arrays.asList("--common"));
        @SuppressWarnings("unchecked")
        CompletableFuture<Process> onExit = Mockito.mock(CompletableFuture.class);

        InputStream ffmpegErrorStream = new ByteArrayInputStream(
                "\njunk\nother\ndata\n1000\n".getBytes(StandardCharsets.UTF_8));

        Mockito.when(this.fileService.getNameWithoutExt(job.getSource())).thenReturn("sleep");
        Mockito.when(this.jsonService.from(job.getProfile(), Profile.class)).thenReturn(profile);
        Mockito.when(this.getConfig().getClusterName()).thenReturn("junit");
        Mockito.when(this.ffmpegProcess.getInputStream()).thenReturn(ffmpegErrorStream);
        Mockito.when(this.ffmpegProcess.onExit()).thenReturn(onExit);

        Mockito.doAnswer(new Answer<String>() {
            @Override
            public String answer(InvocationOnMock invocation) throws Throwable {
                return "/media/clusters/" + invocation.getArgument(0) + "/processing/";
            }
        }).when(this.fileService).getProcessingDir(ArgumentMatchers.notNull());

        ExecHelper helper = new ExecHelper() {
            String[] expected = new String[] { //
                    "ffprobe", //
                    "-v", "error", //
                    "-select_streams", "v:0", //
                    "-count_frames", //
                    "-show_entries", "stream=nb_read_frames", //
                    "-of", "csv=p=0", //
                    "/media/clusters/junit/processing/sleep.avi" };

            @Override
            public void assertCommand(String... command) {
                Assertions.assertEquals(this.expected.length, command.length);
                for (int i = 0; i < this.expected.length; i++) {
                    Assertions.assertEquals(this.expected[i], command[i]);
                }
            }
        };

        this.setField("execHelper", helper, this.service);

        Set<Message> result = this.service.process(job);
        Assertions.assertNotNull(result);
        Assertions.assertTrue(result.isEmpty());
    }

    @Test
    public void test_process_audio_noCommonArgs() {
        Job job = this.fac().job();
        job.setType(JobTypes.Audio);

        Profile profile = this.fac().profile();
        @SuppressWarnings("unchecked")
        CompletableFuture<Process> onExit = Mockito.mock(CompletableFuture.class);

        Mockito.when(this.fileService.getNameWithoutExt(job.getSource())).thenReturn("sleep");
        Mockito.when(this.jsonService.from(job.getProfile(), Profile.class)).thenReturn(profile);
        Mockito.when(this.getConfig().getClusterName()).thenReturn("junit");
        Mockito.when(this.ffmpegProcess.onExit()).thenReturn(onExit);

        Mockito.doAnswer(new Answer<String>() {
            @Override
            public String answer(InvocationOnMock invocation) throws Throwable {
                return "/media/clusters/" + invocation.getArgument(0) + "/processing/";
            }
        }).when(this.fileService).getProcessingDir(ArgumentMatchers.notNull());
        Mockito.doAnswer(new Answer<String>() {
            @Override
            public String answer(InvocationOnMock invocation) throws Throwable {
                return "/media/clusters/" + invocation.getArgument(0) + "/temp/";
            }
        }).when(this.fileService).getTempDir(ArgumentMatchers.notNull());

        ExecHelper helper = new ExecHelper() {
            String[] expected = new String[] { //
                    "ffmpeg", //
                    "-n", //
                    "-loglevel", "error", //
                    "-i", "/media/clusters/junit/processing/sleep.avi", //
                    "-c:a", "copy", //
                    "-vn", //
                    "/media/clusters/junit/temp/sleep-Audio.ts" };

            @Override
            public void assertCommand(String... command) {
                Assertions.assertEquals(this.expected.length, command.length);
                for (int i = 0; i < this.expected.length; i++) {
                    Assertions.assertEquals(this.expected[i], command[i]);
                }
            }
        };

        this.setField("execHelper", helper, this.service);

        Set<Message> result = this.service.process(job);
        Assertions.assertNotNull(result);
        Assertions.assertTrue(result.isEmpty());
    }

    @Test
    public void test_process_audio_withCommonArgs() {
        Job job = this.fac().job();
        job.setType(JobTypes.Audio);

        Profile profile = this.fac().profile();
        profile.setCommonArgs(Arrays.asList("--common"));
        @SuppressWarnings("unchecked")
        CompletableFuture<Process> onExit = Mockito.mock(CompletableFuture.class);

        Mockito.when(this.fileService.getNameWithoutExt(job.getSource())).thenReturn("sleep");
        Mockito.when(this.jsonService.from(job.getProfile(), Profile.class)).thenReturn(profile);
        Mockito.when(this.getConfig().getClusterName()).thenReturn("junit");
        Mockito.when(this.ffmpegProcess.onExit()).thenReturn(onExit);

        Mockito.doAnswer(new Answer<String>() {
            @Override
            public String answer(InvocationOnMock invocation) throws Throwable {
                return "/media/clusters/" + invocation.getArgument(0) + "/processing/";
            }
        }).when(this.fileService).getProcessingDir(ArgumentMatchers.notNull());
        Mockito.doAnswer(new Answer<String>() {
            @Override
            public String answer(InvocationOnMock invocation) throws Throwable {
                return "/media/clusters/" + invocation.getArgument(0) + "/temp/";
            }
        }).when(this.fileService).getTempDir(ArgumentMatchers.notNull());

        ExecHelper helper = new ExecHelper() {
            String[] expected = new String[] { //
                    "ffmpeg", //
                    "-n", //
                    "-loglevel", "error", //
                    "-i", "/media/clusters/junit/processing/sleep.avi", //
                    "--common", //
                    "-c:a", "copy", //
                    "-vn", //
                    "/media/clusters/junit/temp/sleep-Audio.ts" };

            @Override
            public void assertCommand(String... command) {
                Assertions.assertEquals(this.expected.length, command.length);
                for (int i = 0; i < this.expected.length; i++) {
                    Assertions.assertEquals(this.expected[i], command[i]);
                }
            }
        };

        this.setField("execHelper", helper, this.service);

        Set<Message> result = this.service.process(job);
        Assertions.assertNotNull(result);
        Assertions.assertTrue(result.isEmpty());
    }

    @Test
    public void test_process_video_nullArgs() {
        Job job = this.fac().job();
        job.setType(JobTypes.Video);

        Profile profile = this.fac().profile();
        profile.setVideoArgs(null);
        @SuppressWarnings("unchecked")
        CompletableFuture<Process> onExit = Mockito.mock(CompletableFuture.class);

        Mockito.when(this.fileService.getNameWithoutExt(job.getSource())).thenReturn("sleep");
        Mockito.when(this.jsonService.from(job.getProfile(), Profile.class)).thenReturn(profile);
        Mockito.when(this.getConfig().getClusterName()).thenReturn("junit");
        Mockito.when(this.ffmpegProcess.onExit()).thenReturn(onExit);

        Mockito.doAnswer(new Answer<String>() {
            @Override
            public String answer(InvocationOnMock invocation) throws Throwable {
                return "/media/clusters/" + invocation.getArgument(0) + "/processing/";
            }
        }).when(this.fileService).getProcessingDir(ArgumentMatchers.notNull());
        Mockito.doAnswer(new Answer<String>() {
            @Override
            public String answer(InvocationOnMock invocation) throws Throwable {
                return "/media/clusters/" + invocation.getArgument(0) + "/temp/";
            }
        }).when(this.fileService).getTempDir(ArgumentMatchers.notNull());

        ExecHelper helper = new ExecHelper() {
            String[] expected = new String[] { //
                    "ffmpeg", //
                    "-n", //
                    "-loglevel", "error", //
                    "-i", "/media/clusters/junit/processing/sleep.avi", //
                    "-an", //
                    "-vf", "trim=start_frame=10000:end_frame=20000", //
                    "/media/clusters/junit/temp/sleep-Video-00001.ts" };

            @Override
            public void assertCommand(String... command) {
                Assertions.assertEquals(this.expected.length, command.length);
                for (int i = 0; i < this.expected.length; i++) {
                    Assertions.assertEquals(this.expected[i], command[i]);
                }
            }
        };

        this.setField("execHelper", helper, this.service);

        Set<Message> result = this.service.process(job);
        Assertions.assertNotNull(result);
        Assertions.assertTrue(result.isEmpty());
    }

    @Test
    public void test_process_video_emptyArgs() {
        Job job = this.fac().job();
        job.setType(JobTypes.Video);

        Profile profile = this.fac().profile();
        profile.setVideoArgs(new ArrayList<>());
        @SuppressWarnings("unchecked")
        CompletableFuture<Process> onExit = Mockito.mock(CompletableFuture.class);

        Mockito.when(this.fileService.getNameWithoutExt(job.getSource())).thenReturn("sleep");
        Mockito.when(this.jsonService.from(job.getProfile(), Profile.class)).thenReturn(profile);
        Mockito.when(this.getConfig().getClusterName()).thenReturn("junit");
        Mockito.when(this.ffmpegProcess.onExit()).thenReturn(onExit);

        Mockito.doAnswer(new Answer<String>() {
            @Override
            public String answer(InvocationOnMock invocation) throws Throwable {
                return "/media/clusters/" + invocation.getArgument(0) + "/processing/";
            }
        }).when(this.fileService).getProcessingDir(ArgumentMatchers.notNull());
        Mockito.doAnswer(new Answer<String>() {
            @Override
            public String answer(InvocationOnMock invocation) throws Throwable {
                return "/media/clusters/" + invocation.getArgument(0) + "/temp/";
            }
        }).when(this.fileService).getTempDir(ArgumentMatchers.notNull());

        ExecHelper helper = new ExecHelper() {
            String[] expected = new String[] { //
                    "ffmpeg", //
                    "-n", //
                    "-loglevel", "error", //
                    "-i", "/media/clusters/junit/processing/sleep.avi", //
                    "-an", //
                    "-vf", "trim=start_frame=10000:end_frame=20000", //
                    "/media/clusters/junit/temp/sleep-Video-00001.ts" };

            @Override
            public void assertCommand(String... command) {
                Assertions.assertEquals(this.expected.length, command.length);
                for (int i = 0; i < this.expected.length; i++) {
                    Assertions.assertEquals(this.expected[i], command[i]);
                }
            }
        };

        this.setField("execHelper", helper, this.service);

        Set<Message> result = this.service.process(job);
        Assertions.assertNotNull(result);
        Assertions.assertTrue(result.isEmpty());
    }

    @Test
    public void test_process_video_noCommonArgs_noFilters() {
        Job job = this.fac().job();
        job.setType(JobTypes.Video);

        Profile profile = this.fac().profile();
        @SuppressWarnings("unchecked")
        CompletableFuture<Process> onExit = Mockito.mock(CompletableFuture.class);

        Mockito.when(this.fileService.getNameWithoutExt(job.getSource())).thenReturn("sleep");
        Mockito.when(this.jsonService.from(job.getProfile(), Profile.class)).thenReturn(profile);
        Mockito.when(this.getConfig().getClusterName()).thenReturn("junit");
        Mockito.when(this.ffmpegProcess.onExit()).thenReturn(onExit);

        Mockito.doAnswer(new Answer<String>() {
            @Override
            public String answer(InvocationOnMock invocation) throws Throwable {
                return "/media/clusters/" + invocation.getArgument(0) + "/processing/";
            }
        }).when(this.fileService).getProcessingDir(ArgumentMatchers.notNull());
        Mockito.doAnswer(new Answer<String>() {
            @Override
            public String answer(InvocationOnMock invocation) throws Throwable {
                return "/media/clusters/" + invocation.getArgument(0) + "/temp/";
            }
        }).when(this.fileService).getTempDir(ArgumentMatchers.notNull());

        ExecHelper helper = new ExecHelper() {
            String[] expected = new String[] { //
                    "ffmpeg", //
                    "-n", //
                    "-loglevel", "error", //
                    "-i", "/media/clusters/junit/processing/sleep.avi", //
                    "-an", //
                    "-c:v", "copy", //
                    "-vf", "trim=start_frame=10000:end_frame=20000", //
                    "/media/clusters/junit/temp/sleep-Video-00001.ts" };

            @Override
            public void assertCommand(String... command) {
                Assertions.assertEquals(this.expected.length, command.length);
                for (int i = 0; i < this.expected.length; i++) {
                    Assertions.assertEquals(this.expected[i], command[i]);
                }
            }
        };

        this.setField("execHelper", helper, this.service);

        Set<Message> result = this.service.process(job);
        Assertions.assertNotNull(result);
        Assertions.assertTrue(result.isEmpty());
    }

    @Test
    public void test_process_video_withCommonArgsAndFilters() {
        Job job = this.fac().job();
        job.setType(JobTypes.Video);

        Profile profile = this.fac().profile();
        profile.setCommonArgs(Arrays.asList("--common"));
        profile.setVideoArgs(Arrays.asList("-c:v", "copy", "-vf", "scale=-1:1080"));
        @SuppressWarnings("unchecked")
        CompletableFuture<Process> onExit = Mockito.mock(CompletableFuture.class);

        Mockito.when(this.fileService.getNameWithoutExt(job.getSource())).thenReturn("sleep");
        Mockito.when(this.jsonService.from(job.getProfile(), Profile.class)).thenReturn(profile);
        Mockito.when(this.getConfig().getClusterName()).thenReturn("junit");
        Mockito.when(this.ffmpegProcess.onExit()).thenReturn(onExit);

        Mockito.doAnswer(new Answer<String>() {
            @Override
            public String answer(InvocationOnMock invocation) throws Throwable {
                return "/media/clusters/" + invocation.getArgument(0) + "/processing/";
            }
        }).when(this.fileService).getProcessingDir(ArgumentMatchers.notNull());
        Mockito.doAnswer(new Answer<String>() {
            @Override
            public String answer(InvocationOnMock invocation) throws Throwable {
                return "/media/clusters/" + invocation.getArgument(0) + "/temp/";
            }
        }).when(this.fileService).getTempDir(ArgumentMatchers.notNull());

        ExecHelper helper = new ExecHelper() {
            String[] expected = new String[] { //
                    "ffmpeg", //
                    "-n", //
                    "-loglevel", "error", //
                    "-i", "/media/clusters/junit/processing/sleep.avi", //
                    "--common", //
                    "-an", //
                    "-c:v", "copy", //
                    "-vf", "scale=-1:1080,trim=start_frame=10000:end_frame=20000", //
                    "/media/clusters/junit/temp/sleep-Video-00001.ts" };

            @Override
            public void assertCommand(String... command) {
                Assertions.assertEquals(this.expected.length, command.length);
                for (int i = 0; i < this.expected.length; i++) {
                    Assertions.assertEquals(this.expected[i], command[i]);
                }
            }
        };

        this.setField("execHelper", helper, this.service);

        Set<Message> result = this.service.process(job);
        Assertions.assertNotNull(result);
        Assertions.assertTrue(result.isEmpty());
    }

    @Test
    public void test_process_video_withCommonArgsAndMissingFilters() {
        Job job = this.fac().job();
        job.setType(JobTypes.Video);

        Profile profile = this.fac().profile();
        profile.setCommonArgs(Arrays.asList("--common"));
        profile.setVideoArgs(Arrays.asList("-c:v", "copy", "-vf"));
        @SuppressWarnings("unchecked")
        CompletableFuture<Process> onExit = Mockito.mock(CompletableFuture.class);

        Mockito.when(this.fileService.getNameWithoutExt(job.getSource())).thenReturn("sleep");
        Mockito.when(this.jsonService.from(job.getProfile(), Profile.class)).thenReturn(profile);
        Mockito.when(this.getConfig().getClusterName()).thenReturn("junit");
        Mockito.when(this.ffmpegProcess.onExit()).thenReturn(onExit);

        Mockito.doAnswer(new Answer<String>() {
            @Override
            public String answer(InvocationOnMock invocation) throws Throwable {
                return "/media/clusters/" + invocation.getArgument(0) + "/processing/";
            }
        }).when(this.fileService).getProcessingDir(ArgumentMatchers.notNull());
        Mockito.doAnswer(new Answer<String>() {
            @Override
            public String answer(InvocationOnMock invocation) throws Throwable {
                return "/media/clusters/" + invocation.getArgument(0) + "/temp/";
            }
        }).when(this.fileService).getTempDir(ArgumentMatchers.notNull());

        ExecHelper helper = new ExecHelper() {
            String[] expected = new String[] { //
                    "ffmpeg", //
                    "-n", //
                    "-loglevel", "error", //
                    "-i", "/media/clusters/junit/processing/sleep.avi", //
                    "--common", //
                    "-an", //
                    "-c:v", "copy", //
                    "-vf", "trim=start_frame=10000:end_frame=20000", //
                    "/media/clusters/junit/temp/sleep-Video-00001.ts" };

            @Override
            public void assertCommand(String... command) {
                Assertions.assertEquals(this.expected.length, command.length);
                for (int i = 0; i < this.expected.length; i++) {
                    Assertions.assertEquals(this.expected[i], command[i]);
                }
            }
        };

        this.setField("execHelper", helper, this.service);

        Set<Message> result = this.service.process(job);
        Assertions.assertNotNull(result);
        Assertions.assertTrue(result.isEmpty());
    }

    @Test
    public void test_process_video_nullArgs_noSliceLength() {
        Job job = this.fac().job();
        job.setType(JobTypes.Video);

        Profile profile = this.fac().profile();
        profile.setSliceLength(null);
        profile.setVideoArgs(null);
        @SuppressWarnings("unchecked")
        CompletableFuture<Process> onExit = Mockito.mock(CompletableFuture.class);

        Mockito.when(this.fileService.getNameWithoutExt(job.getSource())).thenReturn("sleep");
        Mockito.when(this.jsonService.from(job.getProfile(), Profile.class)).thenReturn(profile);
        Mockito.when(this.getConfig().getClusterName()).thenReturn("junit");
        Mockito.when(this.ffmpegProcess.onExit()).thenReturn(onExit);

        Mockito.doAnswer(new Answer<String>() {
            @Override
            public String answer(InvocationOnMock invocation) throws Throwable {
                return "/media/clusters/" + invocation.getArgument(0) + "/processing/";
            }
        }).when(this.fileService).getProcessingDir(ArgumentMatchers.notNull());
        Mockito.doAnswer(new Answer<String>() {
            @Override
            public String answer(InvocationOnMock invocation) throws Throwable {
                return "/media/clusters/" + invocation.getArgument(0) + "/temp/";
            }
        }).when(this.fileService).getTempDir(ArgumentMatchers.notNull());

        ExecHelper helper = new ExecHelper() {
            String[] expected = new String[] { //
                    "ffmpeg", //
                    "-n", //
                    "-loglevel", "error", //
                    "-i", "/media/clusters/junit/processing/sleep.avi", //
                    "-an", //
                    "/media/clusters/junit/temp/sleep-Video-00001.ts" };

            @Override
            public void assertCommand(String... command) {
                Assertions.assertEquals(this.expected.length, command.length);
                for (int i = 0; i < this.expected.length; i++) {
                    Assertions.assertEquals(this.expected[i], command[i]);
                }
            }
        };

        this.setField("execHelper", helper, this.service);

        Set<Message> result = this.service.process(job);
        Assertions.assertNotNull(result);
        Assertions.assertTrue(result.isEmpty());
    }

    @Test
    public void test_process_video_emptyArgs_noSliceLength() {
        Job job = this.fac().job();
        job.setType(JobTypes.Video);

        Profile profile = this.fac().profile();
        profile.setSliceLength(null);
        profile.setVideoArgs(new ArrayList<>());
        @SuppressWarnings("unchecked")
        CompletableFuture<Process> onExit = Mockito.mock(CompletableFuture.class);

        Mockito.when(this.fileService.getNameWithoutExt(job.getSource())).thenReturn("sleep");
        Mockito.when(this.jsonService.from(job.getProfile(), Profile.class)).thenReturn(profile);
        Mockito.when(this.getConfig().getClusterName()).thenReturn("junit");
        Mockito.when(this.ffmpegProcess.onExit()).thenReturn(onExit);

        Mockito.doAnswer(new Answer<String>() {
            @Override
            public String answer(InvocationOnMock invocation) throws Throwable {
                return "/media/clusters/" + invocation.getArgument(0) + "/processing/";
            }
        }).when(this.fileService).getProcessingDir(ArgumentMatchers.notNull());
        Mockito.doAnswer(new Answer<String>() {
            @Override
            public String answer(InvocationOnMock invocation) throws Throwable {
                return "/media/clusters/" + invocation.getArgument(0) + "/temp/";
            }
        }).when(this.fileService).getTempDir(ArgumentMatchers.notNull());

        ExecHelper helper = new ExecHelper() {
            String[] expected = new String[] { //
                    "ffmpeg", //
                    "-n", //
                    "-loglevel", "error", //
                    "-i", "/media/clusters/junit/processing/sleep.avi", //
                    "-an", //
                    "/media/clusters/junit/temp/sleep-Video-00001.ts" };

            @Override
            public void assertCommand(String... command) {
                Assertions.assertEquals(this.expected.length, command.length);
                for (int i = 0; i < this.expected.length; i++) {
                    Assertions.assertEquals(this.expected[i], command[i]);
                }
            }
        };

        this.setField("execHelper", helper, this.service);

        Set<Message> result = this.service.process(job);
        Assertions.assertNotNull(result);
        Assertions.assertTrue(result.isEmpty());
    }

    @Test
    public void test_process_video_noCommonArgs_noFilters_noSliceLength() {
        Job job = this.fac().job();
        job.setType(JobTypes.Video);

        Profile profile = this.fac().profile();
        profile.setSliceLength(null);
        @SuppressWarnings("unchecked")
        CompletableFuture<Process> onExit = Mockito.mock(CompletableFuture.class);

        Mockito.when(this.fileService.getNameWithoutExt(job.getSource())).thenReturn("sleep");
        Mockito.when(this.jsonService.from(job.getProfile(), Profile.class)).thenReturn(profile);
        Mockito.when(this.getConfig().getClusterName()).thenReturn("junit");
        Mockito.when(this.ffmpegProcess.onExit()).thenReturn(onExit);

        Mockito.doAnswer(new Answer<String>() {
            @Override
            public String answer(InvocationOnMock invocation) throws Throwable {
                return "/media/clusters/" + invocation.getArgument(0) + "/processing/";
            }
        }).when(this.fileService).getProcessingDir(ArgumentMatchers.notNull());
        Mockito.doAnswer(new Answer<String>() {
            @Override
            public String answer(InvocationOnMock invocation) throws Throwable {
                return "/media/clusters/" + invocation.getArgument(0) + "/temp/";
            }
        }).when(this.fileService).getTempDir(ArgumentMatchers.notNull());

        ExecHelper helper = new ExecHelper() {
            String[] expected = new String[] { //
                    "ffmpeg", //
                    "-n", //
                    "-loglevel", "error", //
                    "-i", "/media/clusters/junit/processing/sleep.avi", //
                    "-an", //
                    "-c:v", "copy", //
                    "/media/clusters/junit/temp/sleep-Video-00001.ts" };

            @Override
            public void assertCommand(String... command) {
                Assertions.assertEquals(this.expected.length, command.length);
                for (int i = 0; i < this.expected.length; i++) {
                    Assertions.assertEquals(this.expected[i], command[i]);
                }
            }
        };

        this.setField("execHelper", helper, this.service);

        Set<Message> result = this.service.process(job);
        Assertions.assertNotNull(result);
        Assertions.assertTrue(result.isEmpty());
    }

    @Test
    public void test_process_merge_noCommonArgs() {
        Job job = this.fac().job();
        job.setType(JobTypes.Merge);

        Profile profile = this.fac().profile();
        @SuppressWarnings("unchecked")
        CompletableFuture<Process> onExit = Mockito.mock(CompletableFuture.class);

        Mockito.when(this.fileService.getNameWithoutExt(job.getSource())).thenReturn("sleep");
        Mockito.when(this.jsonService.from(job.getProfile(), Profile.class)).thenReturn(profile);
        Mockito.when(this.getConfig().getClusterName()).thenReturn("junit");
        Mockito.when(this.ffmpegProcess.onExit()).thenReturn(onExit);

        Mockito.doAnswer(new Answer<String>() {
            @Override
            public String answer(InvocationOnMock invocation) throws Throwable {
                return "/media/clusters/" + invocation.getArgument(0) + "/temp/";
            }
        }).when(this.fileService).getTempDir(ArgumentMatchers.notNull());

        ExecHelper helper = new ExecHelper() {
            String[] expected = new String[] { //
                    "ffmpeg", //
                    "-n", //
                    "-loglevel", "error", //
                    "-f", "concat", //
                    "-safe", "0", //
                    "-i", "/media/clusters/junit/temp/sleep-Video.txt", //
                    "-c", "copy", //
                    "/media/clusters/junit/temp/sleep-Video.ts" };

            @Override
            public void assertCommand(String... command) {
                Assertions.assertEquals(this.expected.length, command.length);
                for (int i = 0; i < this.expected.length; i++) {
                    Assertions.assertEquals(this.expected[i], command[i]);
                }
            }
        };

        this.setField("execHelper", helper, this.service);

        Set<Message> result = this.service.process(job);
        Assertions.assertNotNull(result);
        Assertions.assertTrue(result.isEmpty());
    }

    @Test
    public void test_process_merge_withCommonArgs() {
        Job job = this.fac().job();
        job.setType(JobTypes.Merge);

        Profile profile = this.fac().profile();
        profile.setCommonArgs(Arrays.asList("--common"));
        @SuppressWarnings("unchecked")
        CompletableFuture<Process> onExit = Mockito.mock(CompletableFuture.class);

        Mockito.when(this.fileService.getNameWithoutExt(job.getSource())).thenReturn("sleep");
        Mockito.when(this.jsonService.from(job.getProfile(), Profile.class)).thenReturn(profile);
        Mockito.when(this.getConfig().getClusterName()).thenReturn("junit");
        Mockito.when(this.ffmpegProcess.onExit()).thenReturn(onExit);

        Mockito.doAnswer(new Answer<String>() {
            @Override
            public String answer(InvocationOnMock invocation) throws Throwable {
                return "/media/clusters/" + invocation.getArgument(0) + "/temp/";
            }
        }).when(this.fileService).getTempDir(ArgumentMatchers.notNull());

        ExecHelper helper = new ExecHelper() {
            String[] expected = new String[] { //
                    "ffmpeg", //
                    "-n", //
                    "-loglevel", "error", //
                    "-f", "concat", //
                    "-safe", "0", //
                    "-i", "/media/clusters/junit/temp/sleep-Video.txt", //
                    "-c", "copy", //
                    "--common", //
                    "/media/clusters/junit/temp/sleep-Video.ts" };

            @Override
            public void assertCommand(String... command) {
                Assertions.assertEquals(this.expected.length, command.length);
                for (int i = 0; i < this.expected.length; i++) {
                    Assertions.assertEquals(this.expected[i], command[i]);
                }
            }
        };

        this.setField("execHelper", helper, this.service);

        Set<Message> result = this.service.process(job);
        Assertions.assertNotNull(result);
        Assertions.assertTrue(result.isEmpty());
    }

    @Test
    public void test_process_mux_noCommonArgs() {
        Job job = this.fac().job();
        job.setType(JobTypes.Mux);

        Profile profile = this.fac().profile();
        @SuppressWarnings("unchecked")
        CompletableFuture<Process> onExit = Mockito.mock(CompletableFuture.class);

        Mockito.when(this.fileService.getNameWithoutExt(job.getSource())).thenReturn("sleep");
        Mockito.when(this.jsonService.from(job.getProfile(), Profile.class)).thenReturn(profile);
        Mockito.when(this.getConfig().getClusterName()).thenReturn("junit");
        Mockito.when(this.ffmpegProcess.onExit()).thenReturn(onExit);

        Mockito.doAnswer(new Answer<String>() {
            @Override
            public String answer(InvocationOnMock invocation) throws Throwable {
                return "/media/clusters/" + invocation.getArgument(0) + "/temp/";
            }
        }).when(this.fileService).getTempDir(ArgumentMatchers.notNull());
        Mockito.doAnswer(new Answer<String>() {
            @Override
            public String answer(InvocationOnMock invocation) throws Throwable {
                return "/media/clusters/" + invocation.getArgument(0) + "/done/";
            }
        }).when(this.fileService).getDoneDir(ArgumentMatchers.notNull());

        ExecHelper helper = new ExecHelper() {
            String[] expected = new String[] { //
                    "ffmpeg", //
                    "-n", //
                    "-loglevel", "error", //
                    "-i", "/media/clusters/junit/temp/sleep-Audio.ts", //
                    "-i", "/media/clusters/junit/temp/sleep-Video.ts", //
                    "-c", "copy", //
                    "/media/clusters/junit/done/sleep.mkv" };

            @Override
            public void assertCommand(String... command) {
                Assertions.assertEquals(this.expected.length, command.length);
                for (int i = 0; i < this.expected.length; i++) {
                    Assertions.assertEquals(this.expected[i], command[i]);
                }
            }
        };

        this.setField("execHelper", helper, this.service);

        Set<Message> result = this.service.process(job);
        Assertions.assertNotNull(result);
        Assertions.assertTrue(result.isEmpty());
    }

    @Test
    public void test_process_mux_withCommonArgs() {
        Job job = this.fac().job();
        job.setType(JobTypes.Mux);

        Profile profile = this.fac().profile();
        profile.setCommonArgs(Arrays.asList("--common"));
        @SuppressWarnings("unchecked")
        CompletableFuture<Process> onExit = Mockito.mock(CompletableFuture.class);

        Mockito.when(this.fileService.getNameWithoutExt(job.getSource())).thenReturn("sleep");
        Mockito.when(this.jsonService.from(job.getProfile(), Profile.class)).thenReturn(profile);
        Mockito.when(this.getConfig().getClusterName()).thenReturn("junit");
        Mockito.when(this.ffmpegProcess.onExit()).thenReturn(onExit);

        Mockito.doAnswer(new Answer<String>() {
            @Override
            public String answer(InvocationOnMock invocation) throws Throwable {
                return "/media/clusters/" + invocation.getArgument(0) + "/temp/";
            }
        }).when(this.fileService).getTempDir(ArgumentMatchers.notNull());
        Mockito.doAnswer(new Answer<String>() {
            @Override
            public String answer(InvocationOnMock invocation) throws Throwable {
                return "/media/clusters/" + invocation.getArgument(0) + "/done/";
            }
        }).when(this.fileService).getDoneDir(ArgumentMatchers.notNull());

        ExecHelper helper = new ExecHelper() {
            String[] expected = new String[] { //
                    "ffmpeg", //
                    "-n", //
                    "-loglevel", "error", //
                    "-i", "/media/clusters/junit/temp/sleep-Audio.ts", //
                    "-i", "/media/clusters/junit/temp/sleep-Video.ts", //
                    "-c", "copy", //
                    "--common", //
                    "/media/clusters/junit/done/sleep.mkv" };

            @Override
            public void assertCommand(String... command) {
                Assertions.assertEquals(this.expected.length, command.length);
                for (int i = 0; i < this.expected.length; i++) {
                    Assertions.assertEquals(this.expected[i], command[i]);
                }
            }
        };

        this.setField("execHelper", helper, this.service);

        Set<Message> result = this.service.process(job);
        Assertions.assertNotNull(result);
        Assertions.assertTrue(result.isEmpty());
    }
}
