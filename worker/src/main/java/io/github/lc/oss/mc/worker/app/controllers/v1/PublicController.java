package io.github.lc.oss.mc.worker.app.controllers.v1;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.servlet.ModelAndView;

import io.github.lc.oss.commons.serialization.Response;
import io.github.lc.oss.mc.api.Job;
import io.github.lc.oss.mc.api.NodeStatus;
import io.github.lc.oss.mc.api.Status;
import io.github.lc.oss.mc.security.Authorities;
import io.github.lc.oss.mc.worker.app.service.FFMPEGService;
import io.github.lc.oss.mc.worker.security.Configuration;

@Controller
@PreAuthorize(Authorities.PUBLIC)
public class PublicController extends AbstractController {
    @Autowired
    private Configuration config;
    @Autowired
    private FFMPEGService ffmpegService;

    @GetMapping(path = "/", produces = MediaType.TEXT_HTML_VALUE)
    public ModelAndView statusHtml() {
        ModelAndView mv = new ModelAndView("views/status");
        mv.addObject("Cluster_Name", this.config.getClusterName());
        mv.addObject("Node_Name", this.config.getName());
        Job job = this.ffmpegService.getCurrentJob();
        mv.addObject("HasJob", job != null);
        if (job != null) {
            mv.addObject("Job_ID", job.getId());
            mv.addObject("Job_Type", job.getType());
            mv.addObject("Job_Source", job.getSource());
        }
        return mv;
    }

    @GetMapping(path = "/", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Response<NodeStatus>> statusJson() {
        NodeStatus self = new NodeStatus();
        self.setId(this.config.getId());
        self.setClusterName(this.config.getClusterName());
        self.setName(this.config.getName());

        Job job = this.ffmpegService.getCurrentJob();
        if (job == null) {
            self.setStatus(Status.Available);
        } else {
            self.setStatus(Status.InProgress);
            self.setCurrentJob(this.ffmpegService.getCurrentJob());
        }

        return this.respond(self);
    }

    @GetMapping(path = "/error", produces = MediaType.TEXT_HTML_VALUE)
    public ModelAndView error() {
        return new ModelAndView("views/error");
    }
}
