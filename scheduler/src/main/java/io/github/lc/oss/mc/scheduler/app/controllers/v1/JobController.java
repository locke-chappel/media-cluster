package io.github.lc.oss.mc.scheduler.app.controllers.v1;

import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.ModelAndView;

import io.github.lc.oss.commons.api.services.JsonService;
import io.github.lc.oss.commons.encoding.Encodings;
import io.github.lc.oss.commons.jpa.SearchCriteria;
import io.github.lc.oss.commons.jpa.Term;
import io.github.lc.oss.commons.serialization.Message;
import io.github.lc.oss.commons.serialization.PagedResult;
import io.github.lc.oss.commons.serialization.Response;
import io.github.lc.oss.mc.api.Job;
import io.github.lc.oss.mc.api.JobResult;
import io.github.lc.oss.mc.api.JobTypes;
import io.github.lc.oss.mc.api.Messages;
import io.github.lc.oss.mc.api.ServiceResponse;
import io.github.lc.oss.mc.api.SignedRequest;
import io.github.lc.oss.mc.api.Status;
import io.github.lc.oss.mc.scheduler.app.repository.JobRepository;
import io.github.lc.oss.mc.scheduler.app.service.JobSearchService;
import io.github.lc.oss.mc.scheduler.app.service.JobSearchService.JobSearchTerms;
import io.github.lc.oss.mc.scheduler.app.service.JobService;
import io.github.lc.oss.mc.scheduler.app.service.JobStatusService;
import io.github.lc.oss.mc.scheduler.app.service.NodeService;
import io.github.lc.oss.mc.scheduler.app.service.ScheduledJobService;
import io.github.lc.oss.mc.scheduler.app.thread.JobCleaner;
import io.github.lc.oss.mc.scheduler.app.validation.JobResultValidator;
import io.github.lc.oss.mc.scheduler.app.validation.SearchCriteriaValidator;
import io.github.lc.oss.mc.scheduler.app.validation.SignedRequestValidator;
import io.github.lc.oss.mc.security.Authorities;
import io.github.lc.oss.mc.service.FileService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Controller
@PreAuthorize(Authorities.USER)
public class JobController extends AbstractController {
    @Autowired
    private FileService fileService;
    @Autowired
    private JobCleaner jobCleaner;
    @Autowired
    private JobResultValidator jobResultValidator;
    @Autowired
    private JobRepository jobRepo;
    @Autowired
    private JobSearchService jobSearchService;
    @Autowired
    private JobService jobService;
    @Autowired
    private JobStatusService jobStatusService;
    @Autowired
    private JsonService jsonService;
    @Autowired
    private NodeService nodeService;
    @Autowired
    private ScheduledJobService scheduledJobService;
    @Autowired
    private SearchCriteriaValidator searchCriteriaValidator;
    @Autowired
    private SignedRequestValidator signedRequestValidator;

    @GetMapping(path = "/", produces = MediaType.TEXT_HTML_VALUE)
    public ModelAndView jobs(HttpServletRequest request, HttpServletResponse response) {
        return new ModelAndView("views/USER/jobs");
    }

    @Transactional(readOnly = true)
    @GetMapping(path = "/api/v1/jobs", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Response<PagedResult<Job>>> jobSearch( //
            @RequestParam(name = "text", required = false) String text, //
            @RequestParam(name = "sourceSort", required = false) String sourceSort, //
            @RequestParam(name = "typeSort", required = false) String typeSort, //
            @RequestParam(name = "statusSort", required = false) String statusSort, //
            @RequestParam(name = "pageSize", required = false, defaultValue = "10") int pageSize, //
            @RequestParam(name = "pageNumber", required = false, defaultValue = "0") int pageNumber //
    ) {
        SearchCriteria criteria = new SearchCriteria(pageSize, pageNumber, Term.of( //
                Term.of(JobSearchTerms.Source, text, sourceSort), //
                Term.of(JobSearchTerms.Type, text, typeSort), //
                Term.of(JobSearchTerms.Status, text, statusSort)));

        Set<Message> messages = this.searchCriteriaValidator.validate(criteria);
        if (!messages.isEmpty()) {
            return this.respond(messages);
        }

        return this.respond(this.jobSearchService.search(criteria));
    }

    @Transactional(readOnly = true)
    @GetMapping(path = "/api/v1/jobs/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Response<Job>> getJob(@PathVariable("id") String id) {
        ServiceResponse<Job> response = this.jobService.getJob(id);
        if (response.hasMessages()) {
            if (response.hasMessages(Messages.Application.NotFound)) {
                return this.notFound();
            }
            return this.respond(response.getMessages());
        }
        return this.respond(response.getEntity());
    }

    @Transactional(readOnly = true)
    @PutMapping(path = "/api/v1/jobs/{id}", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Response<Job>> saveJob(@PathVariable("id") String id, @RequestBody Job job) {
        job.setId(id);
        ServiceResponse<Job> response = this.jobService.saveScanJob(job);
        if (response.hasMessages()) {
            if (response.hasMessages(Messages.Application.NotFound)) {
                return this.notFound();
            }
            return this.respond(response.getMessages());
        }
        job = response.getEntity();

        this.fileService.moveToProcessing(job.getSource(), job.getClusterName());

        this.scheduledJobService.informNodesOfNewJobs();

        return this.noContent();
    }

    @PreAuthorize(Authorities.PUBLIC)
    @Transactional(readOnly = true)
    @PostMapping(path = "/api/v1/jobs/complete", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Response<?>> completeJob(@RequestBody SignedRequest request) {
        Set<Message> messages = this.signedRequestValidator.validate(request);
        if (!messages.isEmpty()) {
            this.rollback();
            return this.respond(messages);
        }

        JobResult jobResult = this.jsonService.from(Encodings.Base64.decodeString(request.getBody()), JobResult.class);
        messages.addAll(this.jobResultValidator.validate(jobResult));
        if (!messages.isEmpty()) {
            this.rollback();
            return this.respond(messages);
        }

        io.github.lc.oss.mc.scheduler.app.entity.Job job = this.jobRepo.findById(jobResult.getId()).orElse(null);
        if (job == null) {
            this.rollback();
            return this.notFound();
        }

        String otherJobId;
        switch (job.getType()) {
            case Scan:
                ServiceResponse<Job> serviceResponse = this.jobService.createProcessingJobs(jobResult);
                if (serviceResponse.hasMessages()) {
                    this.rollback();
                    return this.respond(messages);
                }
                this.jobStatusService.updateJobStatus(job.getId(), Status.Finished);
                break;
            case Mux:
                this.jobStatusService.updateJobStatus(job.getId(), Status.Complete);
                this.jobCleaner.offer(this.jobService.toModel(job));
                break;
            case Video:
                this.jobStatusService.updateJobStatus(job.getId(), Status.Complete);
                boolean hasMergeBlockingJobs = this.jobRepo.hasMergeBlockingJobs(job.getSource());
                if (!hasMergeBlockingJobs) {
                    otherJobId = this.jobRepo.findJobId(job.getSource(), JobTypes.Merge, Status.Pending);
                    this.jobStatusService.updateJobStatus(otherJobId, Status.Available);
                }
                break;
            case Audio:
            case Merge:
                this.jobStatusService.updateJobStatus(job.getId(), Status.Complete);
                boolean hasMuxBlockingJobs = this.jobRepo.hasMuxBlockingJobs(job.getSource());
                if (!hasMuxBlockingJobs) {
                    otherJobId = this.jobRepo.findJobId(job.getSource(), JobTypes.Mux, Status.Pending);
                    this.jobStatusService.updateJobStatus(otherJobId, Status.Available);
                }
                break;
        }

        this.nodeService.updateNodeStatus(request.getNodeId(), Status.Available);

        this.scheduledJobService.informNodesOfNewJobs();

        return this.noContent();
    }

    @DeleteMapping(path = "/api/v1/jobs/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Response<Job>> deleteJob(@PathVariable("id") String id) {
        ServiceResponse<Job> response = this.jobService.deleteJob(id);
        if (response.hasMessages()) {
            if (response.hasMessages(Messages.Application.NotFound)) {
                return this.notFound();
            }
            return this.respond(response.getMessages());
        }

        return this.noContent();
    }

    @PostMapping(path = "/api/v1/jobs/import", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> importNewJobs() {
        this.jobService.scanForNewFiles();
        return this.noContent();
    }
}
