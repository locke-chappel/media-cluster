package io.github.lc.oss.mc.worker.app.service;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import io.github.lc.oss.commons.api.services.JsonService;
import io.github.lc.oss.commons.encoding.Encodings;
import io.github.lc.oss.commons.serialization.Message;
import io.github.lc.oss.commons.services.AbstractRuntimeService;
import io.github.lc.oss.commons.signing.Algorithms;
import io.github.lc.oss.commons.web.services.HttpService;
import io.github.lc.oss.mc.api.ApiResponse;
import io.github.lc.oss.mc.api.Job;
import io.github.lc.oss.mc.api.JobResult;
import io.github.lc.oss.mc.api.Messages;
import io.github.lc.oss.mc.api.Profile;
import io.github.lc.oss.mc.api.SignedRequest;
import io.github.lc.oss.mc.entity.Constants;
import io.github.lc.oss.mc.service.FileService;
import io.github.lc.oss.mc.worker.security.Configuration;

@Service
public class FFMPEGService extends AbstractRuntimeService {
    private static final Logger logger = LoggerFactory.getLogger(FFMPEGService.class);
    private static final Pattern FRAME_COUNT = Pattern.compile("^(\\d+)$");

    /**
     * Define default FFMPEG arguments that must always be used.<br/>
     * <br/>
     * -n is used to ensure FFMPEG never hangs waiting for user input
     */
    private static String[] FFMPEG_REQUIRED_ARGS = new String[] { //
            "-n", //
            "-loglevel", "error" //
    };

    @Autowired
    private Configuration config;
    @Autowired
    private Environment env;
    @Autowired
    private FileService fileService;
    @Autowired
    private HttpService httpService;
    @Autowired
    private JsonService jsonService;

    @Value("${application.ffmpeg.path:ffmpeg}")
    private String ffmpegPath;
    @Value("${application.ffprobe.path:ffprobe}")
    private String ffprobePath;
    @Value("${application.ffmpeg.postProcessingTimeout:30000}")
    private long postProcessingTimeout;
    @Value("${application.http.retry-count:3}")
    private int retryCount;

    private Process currentProcess;
    private Job currentJob;
    private JobResult currentResult;
    private String[] ffmpegCommand;
    private String[] ffprobeCommand;
    private String nullFile;
    private boolean aborted = false;

    private boolean isIntegrationtest() {
        return this.env.getProperty("integrationtest", Boolean.class, Boolean.FALSE);
    }

    private String getClusterName() {
        return this.config.getClusterName();
    }

    public Job getCurrentJob() {
        return this.currentJob;
    }

    private JobResult getCurrentResult() {
        return this.currentResult;
    }

    private String getProcessingDir() {
        return this.fileService.getProcessingDir(this.getClusterName());
    }

    private String getTempDir() {
        return this.fileService.getTempDir(this.getClusterName());
    }

    private String getDoneDir() {
        return this.fileService.getDoneDir(this.getClusterName());
    }

    /*
     * Exposed for testing
     */
    String getNullFile() {
        if (this.nullFile == null) {
            this.nullFile = this.getFileSeparator().equals("\\") ? "NUL" : "/dev/null";
        }
        return this.nullFile;
    }

    /*
     * Exposed for testing
     */
    String getFileSeparator() {
        return Constants.FILE_SEPARATOR;
    }

    public void abort() {
        if (this.currentProcess != null) {
            this.aborted = true;
            this.currentProcess.destroy();
        }
    }

    public Set<Message> process(Job job) {
        Set<Message> messages = new HashSet<>();

        if (this.currentProcess != null || this.currentJob != null) {
            messages.add(Messages.Application.AlreadyProcessingJob);
            return messages;
        }
        this.currentJob = job;
        this.currentResult = null;
        this.aborted = false;

        String name = this.fileService.getNameWithoutExt(job.getSource());

        Profile profile = this.jsonService.from(job.getProfile(), Profile.class);
        List<String> args = new ArrayList<>();
        String[] baseCommand = this.getFFMPEGCommand();
        switch (job.getType()) {
            case Audio:
                args.add("-i");
                args.add(this.getProcessingDir() + job.getSource());
                if (profile.getCommonArgs() != null) {
                    args.addAll(profile.getCommonArgs());
                }
                args.addAll(profile.getAudioArgs());
                args.add("-vn");
                args.add(this.getTempDir() + name + "-Audio.ts");
                break;
            case Merge:
                args.add("-f");
                args.add("concat");
                args.add("-safe"); // paths are absolute so '-safe 0' is required...
                args.add("0");
                args.add("-i");
                args.add(this.getTempDir() + name + "-Video.txt");
                args.add("-c");
                args.add("copy");
                if (profile.getCommonArgs() != null) {
                    args.addAll(profile.getCommonArgs());
                }
                args.add(this.getTempDir() + name + "-Video.ts");
                break;
            case Mux:
                args.add("-i");
                args.add(this.getTempDir() + name + "-Audio.ts");
                args.add("-i");
                args.add(this.getTempDir() + name + "-Video.ts");
                args.add("-c");
                args.add("copy");
                if (profile.getCommonArgs() != null) {
                    args.addAll(profile.getCommonArgs());
                }
                args.add(this.getDoneDir() + name + "." + profile.getExt());
                break;
            case Scan:
                /*
                 * Note: using count_frames instead of count_packets to ensure accuracy (yes
                 * count_packets is much faster but a packet could in theory contain more than 1
                 * frame).
                 */
                args.add("-v");
                args.add("error");
                args.add("-select_streams");
                args.add("v:0");
                args.add("-count_frames");
                args.add("-show_entries");
                args.add("stream=nb_read_frames");
                args.add("-of");
                args.add("csv=p=0");
                args.add(this.getProcessingDir() + job.getSource());

                baseCommand = this.getFFProbeCommand();
                break;
            case Video:
            default:
                List<String> videoArgs = new ArrayList<>();
                List<String> pArgs = profile.getVideoArgs();

                if (profile.getSliceLength() == null) {
                    if (pArgs != null && !pArgs.isEmpty()) {
                        for (int i = 0; i < pArgs.size(); i++) {
                            String arg = pArgs.get(i);
                            videoArgs.add(arg);
                        }
                    }
                } else {
                    long start = job.getBatchIndex() * profile.getSliceLength();
                    long end = (job.getBatchIndex() + 1) * profile.getSliceLength();
                    boolean hasVideoFilters = false;

                    if (pArgs != null && !pArgs.isEmpty()) {
                        for (int i = 0; i < pArgs.size(); i++) {
                            String arg = pArgs.get(i);
                            if (StringUtils.equals("-vf", arg)) {
                                if (i + 1 < pArgs.size()) {
                                    videoArgs.add(arg);
                                    i++;
                                    hasVideoFilters = true;
                                    String argValue = pArgs.get(i);
                                    videoArgs.add(String.format( //
                                            argValue + ",trim=start_frame=%d:end_frame=%d", start, end));
                                } else {
                                    this.getLogger().warn("-vf argument detected without parameters, ignoring it");
                                }
                            } else {
                                videoArgs.add(arg);
                            }
                        }
                    }

                    if (!hasVideoFilters) {
                        videoArgs.add("-vf");
                        videoArgs.add(String.format("trim=start_frame=%d:end_frame=%d", start, end));
                    }
                }

                args.add("-i");
                args.add(this.getProcessingDir() + job.getSource());
                if (profile.getCommonArgs() != null) {
                    args.addAll(profile.getCommonArgs());
                }
                args.add("-an");
                args.addAll(videoArgs);
                args.add(this.getTempDir() + name + "-" + job.getType() + "-"
                        + String.format("%05d", job.getBatchIndex()) + ".ts");
                break;
        }

        String[] cmd = this.buildCommand(baseCommand, args);
        this.getLogger().info(String.format( //
                "Executing Command: '%s'", //
                Arrays.stream(cmd). //
                        collect(Collectors.joining(" "))));

        this.currentProcess = this.exec(cmd);

        if (this.currentProcess == null) {
            this.currentJob = null;
            this.currentResult = null;
            this.aborted = false;
            messages.add(Messages.Application.FailedToStartJob);
            return messages;
        }

        this.currentProcess.onExit().thenRunAsync(this.waitForResult());

        JobResult result = new JobResult();
        result.setId(job.getId());

        switch (job.getType()) {
            case Scan:
                String frameLine = null;
                try (BufferedReader reader = new BufferedReader(
                        new InputStreamReader(this.currentProcess.getInputStream()))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        frameLine = line;
                    }

                    if (StringUtils.isBlank(frameLine)) {
                        throw new Exception("Unable to capture frame count");
                    } else {
                        Matcher matcher = FFMPEGService.FRAME_COUNT.matcher(frameLine);
                        if (!matcher.find()) {
                            throw new Exception("Unable to capture frame count");
                        }
                        result.setResult(Long.valueOf(matcher.group(1)));
                    }
                } catch (Exception ex) {
                    this.getLogger().error("Error capturing ffprobe output", ex);
                }
                break;
            default:
                // Nothing extra for audio/video/merge jobs
                break;
        }
        this.currentResult = result;

        return messages;
    }

    /*
     * Exposed for testing
     */
    Runnable waitForResult() {
        return new Runnable() {
            @Override
            public void run() {
                FFMPEGService.this.waitUntil(() -> FFMPEGService.this.currentResult != null);
                FFMPEGService.this.onExit();

                FFMPEGService.this.currentJob = null;
                FFMPEGService.this.currentResult = null;
                FFMPEGService.this.currentProcess = null;
                FFMPEGService.this.aborted = false;
            }
        };
    }

    /*
     * Exposed for testing
     */
    String[] getFFMPEGCommand() {
        if (this.ffmpegCommand == null) {
            this.ffmpegCommand = this.buildBaseCommand(this.ffmpegPath, FFMPEGService.FFMPEG_REQUIRED_ARGS);
        }
        return this.ffmpegCommand;
    }

    /*
     * Exposed for testing
     */
    String[] getFFProbeCommand() {
        if (this.ffprobeCommand == null) {
            this.ffprobeCommand = this.buildBaseCommand(this.ffprobePath, new String[0]);
        }
        return this.ffprobeCommand;
    }

    /**
     * Converts a potentially multi-part command into suitable format for
     * ProcessBuilder use.<br/>
     * <br/>
     * Special features:<br/>
     * - escaped spaces ('\ ') in the source path will be preserved in a single
     * argument <br />
     * - Windows path detection - / in file paths will be converted to \ to preserve
     * external process compatibility
     */
    private String[] buildBaseCommand(String command, String[] requiredArgs) {
        String path = command;
        /*
         * preserve any escaped spaced as pre-defined symbol so that they remain a
         * single argument
         */
        path = path.replace("\\ ", "%20");
        /*
         * Split into parts, ProcessBuild requires this
         */
        String[] parts = path.split(" ");
        for (int i = 0; i < parts.length; i++) {
            /*
             * Convert our escaped spaced back to actual spaces
             */
            parts[i] = parts[i].replace("%20", " ");

            /*
             * Special case: if Windows and a file path (e.g. C:/) convert it to backslashes
             * (e.g. C:\) for proper external process compatibility.
             */
            if (StringUtils.equals("\\", this.getFileSeparator()) && parts[i].contains(":")) {
                parts[i] = parts[i].replace("/", this.getFileSeparator());
            }
        }

        /*
         * Add required arguments
         */
        String[] cmd = new String[parts.length + requiredArgs.length];
        System.arraycopy(parts, 0, cmd, 0, parts.length);
        System.arraycopy(requiredArgs, 0, cmd, parts.length, requiredArgs.length);
        return cmd;
    }

    /**
     * Appends arguments to the root command, normalized for OS use.
     */
    private String[] buildCommand(String[] baseCommand, List<String> args) {
        String[] cmd = new String[baseCommand.length + args.size()];
        System.arraycopy(baseCommand, 0, cmd, 0, baseCommand.length);
        System.arraycopy(args.toArray(new String[args.size()]), 0, cmd, baseCommand.length, args.size());
        return cmd;
    }

    private void waitUntil(Supplier<Boolean> condition) {
        final long stopAfter = System.currentTimeMillis() + this.postProcessingTimeout;
        boolean success = false;
        while (!success && System.currentTimeMillis() < stopAfter) {
            success = condition.get();
            try {
                Thread.sleep(10);
            } catch (InterruptedException ex) {
                throw new RuntimeException("Interrupted while waiting for condition.");
            }
        }
    }

    /*
     * Exposed for testing
     */
    void onExit() {
        if (this.aborted) {
            return;
        }

        if (StringUtils.isBlank(this.config.getSchedulerUrl())) {
            /*
             * Note: Scheduler URL will be blank when running in isolated IT mode. This
             * would be an error in an actual deployment.
             */
            if (this.isIntegrationtest()) {
                return;
            } else {
                throw new RuntimeException("Invalid configuration - scheduler URL cannot be blank");
            }
        }

        int tries = 0;
        boolean success = false;
        while (!success) {
            if (tries >= this.retryCount) {
                break;
            }
            try {
                success = this.updateScheduler();
            } catch (RuntimeException ex) {
                this.getLogger().error("Error making REST call to scheduler", ex);
            }
            tries++;
        }
    }

    private boolean updateScheduler() {
        Map<String, String> headers = new HashMap<>();
        headers.put(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE);

        SignedRequest request = new SignedRequest();
        request.setNodeId(this.config.getId());
        request.setBody(Encodings.Base64.encode(this.jsonService.to(this.getCurrentResult())));

        String sig = Algorithms.ED25519.getSignature( //
                this.config.getPrivateKey(), //
                request.getSignatureData());
        request.setSignature(sig);

        String url = this.config.getSchedulerUrl() + "/api/v1/jobs/complete";

        ResponseEntity<ApiResponse> response = this.httpService.call(HttpMethod.POST, url, headers, ApiResponse.class,
                request);
        if (response.getStatusCode() != HttpStatus.NO_CONTENT) {
            if (response.getStatusCode() == HttpStatus.UNPROCESSABLE_ENTITY) {
                ApiResponse body = response.getBody();
                if (body != null) {
                    Collection<Messages> messages = body.getMessages();
                    if (messages != null) {
                        messages.forEach(m -> {
                            this.getLogger()
                                    .error(String.format("Error reporting job completion to scheduler, %s.%s.%d",
                                            m.getCategory(), m.getSeverity(), m.getNumber()));
                        });
                    }
                }
            } else {
                this.getLogger().error(
                        String.format("Error reporting job completion to scheduler: %s", response.getStatusCode()));
            }
            return false;
        }

        return true;
    }

    @Override
    protected Logger getLogger() {
        return FFMPEGService.logger;
    }
}
