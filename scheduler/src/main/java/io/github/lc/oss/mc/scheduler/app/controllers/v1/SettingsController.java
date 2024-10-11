package io.github.lc.oss.mc.scheduler.app.controllers.v1;

import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.servlet.ModelAndView;

import io.github.lc.oss.commons.serialization.Message;
import io.github.lc.oss.commons.serialization.Response;
import io.github.lc.oss.mc.api.ServiceResponse;
import io.github.lc.oss.mc.security.Authorities;
import io.github.lc.oss.mc.scheduler.app.model.EncryptedBackup;
import io.github.lc.oss.mc.scheduler.app.model.User;
import io.github.lc.oss.mc.scheduler.app.service.BackupService;
import io.github.lc.oss.mc.scheduler.app.service.IdentityService;
import io.github.lc.oss.mc.scheduler.app.validation.UserValidator;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Controller
@PreAuthorize(Authorities.USER)
public class SettingsController extends AbstractController {
    @Autowired
    private BackupService backupService;
    @Autowired
    private IdentityService identityService;
    @Autowired
    private UserValidator userValidator;

    @GetMapping(path = "/settings", produces = MediaType.TEXT_HTML_VALUE)
    public ModelAndView settings(HttpServletRequest request, HttpServletResponse response) {
        return new ModelAndView("views/USER/settings");
    }

    @Transactional(readOnly = true)
    @PostMapping(path = "/api/v1/backup", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Response<EncryptedBackup>> downloadBackup(@RequestBody EncryptedBackup backup) {
        ServiceResponse<EncryptedBackup> response = this.backupService.getBackup(backup.getPassword());
        if (response.hasMessages()) {
            return this.respond(response.getMessages());
        }
        return this.respond(response.getEntity());
    }

    @PutMapping(path = "/api/v1/backup", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Response<?>> restoreBackup(@RequestBody EncryptedBackup backup) {
        ServiceResponse<EncryptedBackup> response = this.backupService.restoreBackup(backup);
        if (response.hasMessages()) {
            return this.respond(response.getMessages());
        }
        return this.noContent();
    }

    @PutMapping(path = "/api/v1/user", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Response<?>> updateUser(@RequestBody User user) {
        if (!this.identityService.isSelfIssuing()) {
            return this.notFound();
        }

        Set<Message> messages = this.userValidator.validate(user);
        if (!messages.isEmpty()) {
            this.rollback();
            return this.respond(messages);
        }

        this.identityService.updateUser(user);

        return this.noContent();
    }
}
