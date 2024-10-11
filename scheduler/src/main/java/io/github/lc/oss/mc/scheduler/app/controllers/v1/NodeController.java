package io.github.lc.oss.mc.scheduler.app.controllers.v1;

import java.util.List;
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

import io.github.lc.oss.commons.jpa.SearchCriteria;
import io.github.lc.oss.commons.jpa.Term;
import io.github.lc.oss.commons.serialization.JsonableCollection;
import io.github.lc.oss.commons.serialization.JsonableHashSet;
import io.github.lc.oss.commons.serialization.Message;
import io.github.lc.oss.commons.serialization.PagedResult;
import io.github.lc.oss.commons.serialization.Primitive;
import io.github.lc.oss.commons.serialization.Response;
import io.github.lc.oss.mc.api.Messages;
import io.github.lc.oss.mc.api.ServiceResponse;
import io.github.lc.oss.mc.security.Authorities;
import io.github.lc.oss.mc.scheduler.app.model.Node;
import io.github.lc.oss.mc.scheduler.app.repository.NodeRepository;
import io.github.lc.oss.mc.scheduler.app.service.NodeSearchService;
import io.github.lc.oss.mc.scheduler.app.service.NodeService;
import io.github.lc.oss.mc.scheduler.app.service.ScheduledJobService;
import io.github.lc.oss.mc.scheduler.app.service.NodeSearchService.NodeSearchTerms;
import io.github.lc.oss.mc.scheduler.app.validation.NodeValidator;
import io.github.lc.oss.mc.scheduler.app.validation.SearchCriteriaValidator;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Controller
@PreAuthorize(Authorities.USER)
public class NodeController extends AbstractController {
    @Autowired
    private NodeRepository nodeRepo;
    @Autowired
    private NodeSearchService nodeSearchService;
    @Autowired
    private NodeService nodeService;
    @Autowired
    private NodeValidator nodeValidator;
    @Autowired
    private ScheduledJobService scheduledJobService;
    @Autowired
    private SearchCriteriaValidator searchCriteriaValidator;

    @GetMapping(path = "/nodes", produces = MediaType.TEXT_HTML_VALUE)
    public ModelAndView nodes(HttpServletRequest request, HttpServletResponse response) {
        return new ModelAndView("views/USER/nodes");
    }

    @Transactional(readOnly = true)
    @GetMapping(path = "/api/v1/nodes", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Response<PagedResult<Node>>> nodeSearch( //
            @RequestParam(name = "text", required = false) String text, //
            @RequestParam(name = "clusterNameSort", required = false) String clusterNameSort, //
            @RequestParam(name = "nameSort", required = false) String nameSort, //
            @RequestParam(name = "status", required = false) String statusSort, //
            @RequestParam(name = "urlSort", required = false) String urlSort, //
            @RequestParam(name = "pageSize", required = false, defaultValue = "10") int pageSize, //
            @RequestParam(name = "pageNumber", required = false, defaultValue = "0") int pageNumber //
    ) {
        SearchCriteria criteria = new SearchCriteria(pageSize, pageNumber, Term.of( //
                Term.of(NodeSearchTerms.ClusterName, text, clusterNameSort), //
                Term.of(NodeSearchTerms.Name, text, nameSort), //
                Term.of(NodeSearchTerms.Status, text, statusSort), //
                Term.of(NodeSearchTerms.URL, text, urlSort)));

        Set<Message> messages = this.searchCriteriaValidator.validate(criteria);
        if (!messages.isEmpty()) {
            return this.respond(messages);
        }

        return this.respond(this.nodeSearchService.search(criteria));
    }

    @Transactional(readOnly = true)
    @GetMapping(path = "/api/v1/nodes/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Response<Node>> getNode(@PathVariable("id") String id) {
        ServiceResponse<Node> response = this.nodeService.getNode(id);
        if (response.hasMessages()) {
            if (response.hasMessages(Messages.Application.NotFound)) {
                return this.notFound();
            }
            return this.respond(response.getMessages());
        }
        return this.respond(response.getEntity());
    }

    @Transactional
    @PostMapping(path = "/api/v1/nodes", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Response<Node>> createNode(@RequestBody Node request) {
        request.setId(null);
        request.setModified(null);

        return this.saveNode(request);
    }

    @Transactional
    @PutMapping(path = "/api/v1/nodes/{id}", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Response<Node>> updateNode(@PathVariable("id") String id, @RequestBody Node request) {
        request.setId(id);

        return this.saveNode(request);
    }

    private ResponseEntity<Response<Node>> saveNode(Node request) {
        Set<Message> messages = this.nodeValidator.validate(request);
        if (!messages.isEmpty()) {
            this.rollback();
            return this.respond(messages);
        }

        ServiceResponse<Node> response = this.nodeService.saveNode(request);
        if (response.hasMessages()) {
            this.rollback();
            return this.respond(response.getMessages());
        }

        if (response.getEntity() == null) {
            this.rollback();
            return this.notFound();
        }

        return this.respond(response.getEntity());
    }

    @PostMapping(path = "/api/v1/nodes/{id}/config", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Response<Node>> newNodeConfig(@PathVariable("id") String nodeId) {
        ServiceResponse<Node> response = this.nodeService.newConfig(nodeId);
        if (response.hasMessages()) {
            return this.respond(response.getMessages());
        }

        if (response.getEntity() == null) {
            return this.notFound();
        }

        return this.respond(response.getEntity());
    }

    @Transactional
    @DeleteMapping(path = "/api/v1/nodes/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Response<Node>> deleteNode(@PathVariable("id") String id) {
        io.github.lc.oss.mc.scheduler.app.entity.Node existing = this.nodeRepo.findById(id).orElse(null);
        if (existing == null) {
            return this.notFound();
        }

        this.nodeRepo.delete(existing);

        return this.noContent();
    }

    @Transactional(readOnly = true)
    @GetMapping(path = "/api/v1/nodes/clusters", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Response<JsonableCollection<Primitive<String>>>> getClusters() {
        List<String> clusterNames = this.nodeRepo.findAllDistinctClusterNames();

        JsonableHashSet<Primitive<String>> values = new JsonableHashSet<>(clusterNames, s -> new Primitive<>(s));
        return this.respond(values);
    }

    @Transactional(readOnly = true)
    @PostMapping(path = "/api/v1/nodes/inform", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> informNodes() {
        this.scheduledJobService.informNodesOfNewJobs();
        return this.noContent();
    }

    @Transactional(readOnly = true)
    @PostMapping(path = "/api/v1/nodes/refresh", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> refreshNodeStatus() {
        this.scheduledJobService.updateNodeStatuses();
        return this.noContent();
    }
}
