package io.github.lc.oss.mc.scheduler.app.controllers.v1;

import java.util.List;
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;

import io.github.lc.oss.commons.serialization.JsonableCollection;
import io.github.lc.oss.commons.serialization.JsonableHashSet;
import io.github.lc.oss.commons.serialization.Message;
import io.github.lc.oss.commons.serialization.PrimitiveKeyValue;
import io.github.lc.oss.commons.serialization.Response;
import io.github.lc.oss.mc.api.Messages;
import io.github.lc.oss.mc.api.ServiceResponse;
import io.github.lc.oss.mc.security.Authorities;
import io.github.lc.oss.mc.scheduler.app.model.Profile;
import io.github.lc.oss.mc.scheduler.app.repository.ProfileRepository;
import io.github.lc.oss.mc.scheduler.app.service.ProfileService;
import io.github.lc.oss.mc.scheduler.app.validation.ProfileValidator;

@Controller
@PreAuthorize(Authorities.USER)
public class ProfileController extends AbstractController {
    @Autowired
    private ProfileRepository profileRepo;
    @Autowired
    private ProfileService profileService;
    @Autowired
    private ProfileValidator profileValidator;

    @Transactional(readOnly = true)
    @GetMapping(path = "/api/v1/profiles", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Response<JsonableCollection<PrimitiveKeyValue<String, String>>>> profiles() {
        List<io.github.lc.oss.mc.scheduler.app.entity.Profile> profiles = this.profileRepo.findAll();
        return this.respond(new JsonableHashSet<PrimitiveKeyValue<String, String>>(profiles,
                p -> new PrimitiveKeyValue<>(p.getId(), p.getName())));
    }

    @Transactional(readOnly = true)
    @GetMapping(path = "/api/v1/profiles/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Response<Profile>> profile(@PathVariable("id") String id) {
        ServiceResponse<Profile> response = this.profileService.getProfile(id);
        if (response.hasMessages()) {
            if (response.hasMessages(Messages.Application.NotFound)) {
                return this.notFound();
            }
            return this.respond(response.getMessages());
        }

        return this.respond(response.getEntity());
    }

    @Transactional
    @PostMapping(path = "/api/v1/profiles", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Response<Profile>> createProfile(@RequestBody Profile request) {
        request.setId(null);
        request.setModified(null);

        return this.saveProfile(request);
    }

    @Transactional
    @PutMapping(path = "/api/v1/profiles/{id}", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Response<Profile>> updateProfile(@PathVariable("id") String id,
            @RequestBody Profile request) {
        request.setId(id);

        return this.saveProfile(request);
    }

    private ResponseEntity<Response<Profile>> saveProfile(Profile request) {
        Set<Message> messages = this.profileValidator.validate(request);
        if (!messages.isEmpty()) {
            return this.respond(messages);
        }

        ServiceResponse<Profile> response = this.profileService.saveProfile(request);
        if (response.hasMessages()) {
            return this.respond(response.getMessages());
        }

        if (response.getEntity() == null) {
            return this.notFound();
        }

        return this.respond(response.getEntity());
    }
}
