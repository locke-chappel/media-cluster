package io.github.lc.oss.mc.scheduler.app.service;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import io.github.lc.oss.commons.api.services.JsonService;
import io.github.lc.oss.commons.encoding.Encodings;
import io.github.lc.oss.commons.l10n.Variable;
import io.github.lc.oss.commons.web.annotations.SystemIdentity;
import io.github.lc.oss.commons.web.services.HttpService;
import io.github.lc.oss.mc.api.ApiResponse;
import io.github.lc.oss.mc.api.JobResult;
import io.github.lc.oss.mc.api.JobTypes;
import io.github.lc.oss.mc.api.Messages;
import io.github.lc.oss.mc.api.ServiceResponse;
import io.github.lc.oss.mc.api.SignedRequest;
import io.github.lc.oss.mc.api.Status;
import io.github.lc.oss.mc.scheduler.app.entity.Job;
import io.github.lc.oss.mc.scheduler.app.entity.Node;
import io.github.lc.oss.mc.scheduler.app.model.NodeStatusResponse;
import io.github.lc.oss.mc.scheduler.app.model.Profile;
import io.github.lc.oss.mc.scheduler.app.repository.JobRepository;
import io.github.lc.oss.mc.scheduler.app.repository.NodeRepository;
import io.github.lc.oss.mc.scheduler.app.thread.JobCleaner;
import io.github.lc.oss.mc.scheduler.app.thread.JobCreator;
import io.github.lc.oss.mc.service.FileService;

@Service
public class JobService extends AbstractService {
    @Autowired
    private FileService fileService;
    @Autowired
    private HttpService httpService;
    @Autowired
    private JobRepository jobRepo;
    @Autowired
    private JsonService jsonService;
    @Autowired
    private NodeRepository nodeRepo;
    @Autowired
    private NodeService nodeService;
    @Autowired
    private ProfileService profileService;
    @Autowired
    private SignatureService signatureService;
    @Autowired
    private JobCreator jobCreator;
    @Autowired
    private JobEntityService jobEntityService;
    @Autowired
    private ScheduledJobService scheduledJobService;
    @Autowired
    private JobCleaner jobCleaner;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public ServiceResponse<io.github.lc.oss.mc.api.Job> abortJob(String nodeId) {
        ServiceResponse<io.github.lc.oss.mc.api.Job> response = new ServiceResponse<>();

        Node node = this.nodeRepo.findById(nodeId).orElse(null);
        if (node == null) {
            this.rollback();
            this.addMessage(response, Messages.Application.NotFound);
            return response;
        }

        if (node.getStatus() != Status.InProgress) {
            this.rollback();
            this.addMessage(response, Messages.Application.NodeNotBusy);
            return response;
        }

        SignedRequest request = new SignedRequest();
        request.setNodeId(node.getId());
        request.setBody("abort");

        this.signatureService.sign(request);

        Map<String, String> headers = new HashMap<>();
        headers.put(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE);

        ResponseEntity<ApiResponse> workerResponse = this.httpService.call(HttpMethod.DELETE,
                node.getUrl() + "/api/v1/jobs", headers, ApiResponse.class, request);
        if (workerResponse.getStatusCode() != HttpStatus.ACCEPTED) {
            this.rollback();

            if (workerResponse.getStatusCode() == HttpStatus.UNPROCESSABLE_ENTITY) {
                ApiResponse body = workerResponse.getBody();
                if (body != null) {
                    Collection<Messages> messages = body.getMessages();
                    if (messages != null) {
                        messages.forEach(m -> {
                            this.getLogger().error(String.format("Error aborting job on worker %s (%s), %s.%s.%d",
                                    node.getName(), node.getUrl(), m.getCategory(), m.getSeverity(), m.getNumber()));
                        });
                    }
                }
            }
            this.addMessage(response, Messages.Application.ErrorAbortingJob);
            return response;
        }

        node.setStatus(Status.Available);

        return response;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public ServiceResponse<io.github.lc.oss.mc.api.Job> dispatchJob(String nodeId) {
        ServiceResponse<io.github.lc.oss.mc.api.Job> response = new ServiceResponse<>();

        Node node = this.nodeRepo.findById(nodeId).orElse(null);
        if (node == null) {
            this.rollback();
            this.addMessage(response, Messages.Application.NotFound);
            return response;
        }

        if (node.getStatus() != Status.Available) {
            this.rollback();
            this.addMessage(response, Messages.Application.NodeNotAvailable);
            return response;
        }

        Job job = this.jobEntityService.findNextJobForCluster(node.getClusterName(), node.getAllowedJobTypes());
        if (job == null) {
            this.rollback();
            this.addMessage(response, Messages.Application.NoJobsAvailable);
            return response;
        }

        if (job.getType() == JobTypes.Merge) {
            List<Job> incompleteJobs = this.jobRepo.findMergeBlockingJobs(job.getSource());
            if (!incompleteJobs.isEmpty()) {
                /*
                 * Our next job is a merge job but there are in-progress Video jobs still
                 * processing. Skip the merge job for now and move on to the next job. Since the
                 * find next job logic looks at the lowest index for the cluster (filtering by
                 * types) the merge job will again be a candidate for the next available worker.
                 *
                 * Also skip any Mux jobs as we can't Mux until Merge is complete.
                 */
                Set<JobTypes> jobTypes = new HashSet<>(node.getAllowedJobTypes());
                jobTypes.remove(JobTypes.Merge);
                jobTypes.remove(JobTypes.Mux);
                job = this.jobEntityService.findNextJobForCluster(node.getClusterName(), jobTypes);
                if (job == null) {
                    this.rollback();
                    this.addMessage(response, Messages.Application.NoJobsAvailable);
                    return response;
                }
            }
        } else if (job.getType() == JobTypes.Mux) {
            List<Job> incompleteJobs = this.jobRepo.findMuxBlockingJobs(job.getSource());
            if (!incompleteJobs.isEmpty()) {
                /*
                 * Our next job is a mux job but there are in-progress Audio/Video jobs still
                 * processing. Skip the mux job for now and move on to the next job. Since the
                 * find next job logic looks at the lowest index for the cluster (filtering by
                 * types) the mux job will again be a candidate for the next available worker.
                 */
                Set<JobTypes> jobTypes = new HashSet<>(node.getAllowedJobTypes());
                jobTypes.remove(JobTypes.Mux);
                job = this.jobEntityService.findNextJobForCluster(node.getClusterName(), jobTypes);
                if (job == null) {
                    this.rollback();
                    this.addMessage(response, Messages.Application.NoJobsAvailable);
                    return response;
                }
            }
        }

        SignedRequest request = new SignedRequest();
        request.setNodeId(node.getId());

        io.github.lc.oss.mc.api.Job j = new io.github.lc.oss.mc.api.Job();
        j.setId(job.getId());
        j.setType(job.getType());
        j.setSource(job.getSource());
        j.setProfile(job.getProfile());
        j.setBatchIndex(job.getBatchIndex());
        request.setBody(Encodings.Base64.encode(this.jsonService.to(j)));

        this.signatureService.sign(request);

        Map<String, String> headers = new HashMap<>();
        headers.put(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE);

        ResponseEntity<ApiResponse> workerResponse = this.httpService.call(HttpMethod.POST,
                node.getUrl() + "/api/v1/jobs", headers, ApiResponse.class, request);
        if (workerResponse.getStatusCode() != HttpStatus.ACCEPTED) {
            this.rollback();

            if (workerResponse.getStatusCode() == HttpStatus.UNPROCESSABLE_ENTITY) {
                ApiResponse body = workerResponse.getBody();
                if (body != null) {
                    Collection<Messages> messages = body.getMessages();
                    if (messages != null) {
                        messages.forEach(m -> {
                            this.getLogger().error(String.format("Error dispatching job to worker %s (%s), %s.%s.%d",
                                    node.getName(), node.getUrl(), m.getCategory(), m.getSeverity(), m.getNumber()));
                        });
                    }
                }
            }
            this.addMessage(response, Messages.Application.ErrorDispatchingJob);
            return response;
        }

        job.setStatus(Status.InProgress);
        node.setStatus(Status.InProgress);

        return response;
    }

    @Transactional(readOnly = true)
    public ServiceResponse<io.github.lc.oss.mc.api.Job> createProcessingJobs(JobResult jobResult) {
        ServiceResponse<io.github.lc.oss.mc.api.Job> response = new ServiceResponse<>();
        Job parent = this.jobRepo.findById(jobResult.getId()).orElse(null);
        if (parent == null) {
            this.addMessage(response, Messages.Application.NotFound);
            return response;
        }

        if (parent.getType() != JobTypes.Scan) {
            this.addMessage(response, Messages.Application.InvalidJobType);
            return response;
        }

        long frames = jobResult.getResult();
        Profile profile = this.jsonService.from(parent.getProfile(), Profile.class);

        long slices;
        if (profile.getSliceLength() == null) {
            slices = 1;
        } else {
            slices = frames / profile.getSliceLength();
            long remainder = frames % profile.getSliceLength();
            if (remainder >= 0) {
                slices++;
            }
        }

        String name = this.fileService.getNameWithoutExt(parent.getSource());

        List<io.github.lc.oss.mc.api.Job> newJobs = new ArrayList<>();
        if (profile.hasAudio()) {
            io.github.lc.oss.mc.api.Job job = new io.github.lc.oss.mc.api.Job();
            job.setSource(parent.getSource());
            job.setProfile(parent.getProfile());
            job.setType(JobTypes.Audio);
            job.setStatus(Status.Available);
            job.setClusterName(parent.getClusterName());
            job.setBatchIndex(0);
            newJobs.add(job);
        }

        if (profile.hasVideo()) {
            StringBuilder videoList = new StringBuilder();
            for (int i = 0; i < slices; i++) {
                io.github.lc.oss.mc.api.Job job = new io.github.lc.oss.mc.api.Job();
                job.setSource(parent.getSource());
                job.setProfile(parent.getProfile());
                job.setType(JobTypes.Video);
                job.setStatus(Status.Available);
                job.setClusterName(parent.getClusterName());
                job.setBatchIndex(i);
                newJobs.add(job);

                videoList.append(String.format("file '%s-video-%05d.ts'\n", name, job.getBatchIndex()));
            }

            String videoListFileName = this.fileService.getTempDir(parent.getClusterName()) + name + "-Video.txt";
            try (BufferedWriter writer = new BufferedWriter(new FileWriter(videoListFileName))) {
                writer.write(videoList.toString());
            } catch (IOException ex) {
                this.rollback();
                this.getLogger().error("Error writing video list to file " + videoListFileName, ex);
                this.addMessage(response, Messages.Application.ErrorWritingVideoList);
                return response;
            }

            io.github.lc.oss.mc.api.Job job = new io.github.lc.oss.mc.api.Job();
            job.setSource(parent.getSource());
            job.setProfile(parent.getProfile());
            job.setType(JobTypes.Merge);
            job.setStatus(Status.Pending);
            job.setClusterName(parent.getClusterName());
            job.setBatchIndex(0);
            newJobs.add(job);
        }

        io.github.lc.oss.mc.api.Job job = new io.github.lc.oss.mc.api.Job();
        job.setSource(parent.getSource());
        job.setProfile(parent.getProfile());
        job.setType(JobTypes.Mux);
        job.setStatus(Status.Pending);
        job.setClusterName(parent.getClusterName());
        job.setBatchIndex(0);
        newJobs.add(job);

        this.jobCreator.offer(newJobs);

        return response;
    }

    @Transactional
    public void scanForNewFiles() {
        List<String> toSchedule = this.fileService.findNewFiles();
        for (String source : toSchedule) {
            this.newFile(source);
        }
    }

    @SystemIdentity
    @Transactional
    public void newFile(String source) {
        if (!this.jobRepo.existsBySourceIgnoreCase(source)) {
            Job job = new Job();
            job.setIndex(null);
            job.setType(JobTypes.Scan);
            job.setSource(source);
            job.setStatus(Status.Pending);

            this.jobRepo.save(job);
        }
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public ServiceResponse<io.github.lc.oss.mc.api.Job> saveScanJob(io.github.lc.oss.mc.api.Job job) {
        ServiceResponse<io.github.lc.oss.mc.api.Job> response = new ServiceResponse<>();

        io.github.lc.oss.mc.scheduler.app.entity.Job existing = this.jobRepo.findById(job.getId()).orElse(null);
        if (existing == null) {
            this.rollback();
            this.addMessage(response, Messages.Application.NotFound);
            return response;
        }

        if (existing.getType() != JobTypes.Scan) {
            this.rollback();
            this.addMessage(response, Messages.Application.InvalidJobType);
            return response;
        }

        if (existing.getStatus() != Status.Pending) {
            this.rollback();
            this.addMessage(response, Messages.Application.InvalidJobStatus);
            return response;
        }

        ServiceResponse<Profile> profileResponse = this.profileService.validateJson(job.getProfile());
        if (profileResponse.hasMessages()) {
            this.rollback();
            response.addMessages(profileResponse.getMessages());
            return response;
        }

        Profile profile = profileResponse.getEntity();
        if (!this.nodeService.canClusterProcess(job.getClusterName(), profile.hasAudio(), profile.hasVideo())) {
            this.rollback();
            this.addMessage(response, Messages.Application.UnprocessableJob);
            return response;
        }

        /*
         * Note: intentionally passing job.profile though de/serialization - this will
         * remove any unknown JSON properties and help ensure safe JSON payloads
         */
        existing.setProfile(this.jsonService.to(profile));
        existing.setClusterName(job.getClusterName());
        existing.setStatus(Status.Available);
        existing.setIndex(this.jobEntityService.findNextJobIndexForCluster(existing.getClusterName()));
        existing.setBatchIndex(0);

        response.setEntity(this.toModel(existing));
        return response;
    }

    @Transactional(readOnly = true)
    public ServiceResponse<io.github.lc.oss.mc.api.Job> getJob(String id) {
        ServiceResponse<io.github.lc.oss.mc.api.Job> response = new ServiceResponse<>();
        io.github.lc.oss.mc.scheduler.app.entity.Job existing = this.jobRepo.findById(id).orElse(null);
        if (existing == null) {
            this.addMessage(response, Messages.Application.NotFound);
            return response;
        }

        io.github.lc.oss.mc.api.Job job = this.toModel(existing);
        if (existing.getType() == JobTypes.Scan && existing.getStatus() == Status.Finished) {
            job.setStatus(Status.InProgress);
            long count = this.jobRepo.countBySourceAndStatusIn(existing.getSource(), Status.Complete);
            long total = this.jobRepo.countBySource(existing.getSource());
            String statusMessage = this.getText( //
                    Messages.Application.JobBatchStatus, //
                    new Variable("Count", Long.toString(count)), //
                    new Variable("Total", Long.toString(total)));
            job.setStatusMessage(statusMessage);
        }

        response.setEntity(job);
        return response;
    }

    @Transactional
    public ServiceResponse<io.github.lc.oss.mc.api.Job> deleteJob(String id) {
        ServiceResponse<io.github.lc.oss.mc.api.Job> response = new ServiceResponse<>();

        io.github.lc.oss.mc.scheduler.app.entity.Job existing = this.jobRepo.findById(id).orElse(null);
        if (existing == null) {
            this.rollback();
            this.addMessage(response, Messages.Application.NotFound);
            return response;
        }

        this.jobRepo.delete(existing);
        this.jobRepo.deleteBySourceIgnoreCase(existing.getSource());

        List<Node> nodes = this.nodeRepo.findByClusterNameIgnoreCase(existing.getClusterName());
        nodes.stream().parallel().forEach(node -> {
            Map<String, String> headers = new HashMap<>();
            headers.put(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE);

            try {
                ResponseEntity<NodeStatusResponse> workerResponse = this.httpService.call( //
                        HttpMethod.GET, //
                        node.getUrl(), //
                        headers, //
                        NodeStatusResponse.class, //
                        null);
                if (workerResponse.getStatusCode() == HttpStatus.OK
                        && workerResponse.getBody().getBody().getCurrentJob() != null) {
                    String currentSource = workerResponse.getBody().getBody().getCurrentJob().getSource();
                    if (StringUtils.equals(currentSource, existing.getSource())) {
                        this.abortJob(node.getId());
                    }
                }
            } catch (Throwable ex) {
                /*
                 * The purpose of this processing is to abort any active jobs so if the worker
                 * cannot be reached it almost certainly is not processing anything (or at least
                 * anything worth keeping). We can therefore safely ignore connection issues.
                 */
                if (!StringUtils.containsIgnoreCase(ex.getMessage(), "Connection refused")) {
                    throw ex;
                }
            }
        });

        if (StringUtils.isNotBlank(existing.getClusterName())) {
            String name = this.fileService.getNameWithoutExt(existing.getSource());

            List<String> processingFiles = this.fileService.findProcessingFiles(name, existing.getClusterName());
            for (String file : processingFiles) {
                if (StringUtils.equals(file, existing.getSource())) {
                    this.fileService.moveToNew(existing.getSource(), existing.getClusterName());
                }
            }

            this.jobCleaner.offer(this.toModel(existing));
        }

        this.scheduledJobService.informNodesOfNewJobs();

        return response;
    }

    public io.github.lc.oss.mc.api.Job toModel(Job job) {
        io.github.lc.oss.mc.api.Job j = new io.github.lc.oss.mc.api.Job();
        j.setClusterName(job.getClusterName());
        j.setId(job.getId());
        j.setIndex(job.getIndex());
        j.setType(job.getType());
        j.setSource(job.getSource());
        j.setStatus(job.getStatus());
        j.setProfile(job.getProfile());
        return j;
    }
}
