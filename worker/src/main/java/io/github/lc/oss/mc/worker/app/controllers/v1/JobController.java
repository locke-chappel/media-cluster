package io.github.lc.oss.mc.worker.app.controllers.v1;

import java.util.Arrays;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import io.github.lc.oss.commons.api.services.JsonService;
import io.github.lc.oss.commons.encoding.Encodings;
import io.github.lc.oss.commons.serialization.Message;
import io.github.lc.oss.mc.api.Job;
import io.github.lc.oss.mc.api.Messages;
import io.github.lc.oss.mc.api.SignedRequest;
import io.github.lc.oss.mc.security.Authorities;
import io.github.lc.oss.mc.worker.app.service.FFMPEGService;
import io.github.lc.oss.mc.worker.app.validation.JobRequestValidator;
import io.github.lc.oss.mc.worker.app.validation.SignedRequestValidator;

@Controller
@PreAuthorize(Authorities.PUBLIC)
public class JobController extends AbstractController {
    @Autowired
    private FFMPEGService ffmpegService;
    @Autowired
    private JobRequestValidator jobRequestValidator;
    @Autowired
    private JsonService jsonService;
    @Autowired
    private SignedRequestValidator signedRequestValidator;

    @PostMapping(path = "/api/v1/jobs", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> startJob(@RequestBody SignedRequest request) {
        Set<Message> messages = this.signedRequestValidator.validate(request);
        if (!messages.isEmpty()) {
            return this.respond(messages);
        }

        Job job = this.jsonService.from(Encodings.Base64.decodeString(request.getBody()), Job.class);
        messages.addAll(this.jobRequestValidator.validate(job));
        if (!messages.isEmpty()) {
            return this.respond(messages);
        }

        messages.addAll(this.ffmpegService.process(job));
        if (!messages.isEmpty()) {
            return this.respond(messages);
        }

        return new ResponseEntity<>(HttpStatus.ACCEPTED);
    }

    @DeleteMapping(path = "/api/v1/jobs", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> abortJob(@RequestBody SignedRequest request) {
        Set<Message> messages = this.signedRequestValidator.validate(request);
        if (!messages.isEmpty()) {
            return this.respond(messages);
        }

        if (!StringUtils.equalsIgnoreCase(request.getBody(), "abort")) {
            return this.respond(Arrays.asList(Messages.Application.InvalidField));
        }

        this.ffmpegService.abort();

        return new ResponseEntity<>(HttpStatus.ACCEPTED);
    }
}
