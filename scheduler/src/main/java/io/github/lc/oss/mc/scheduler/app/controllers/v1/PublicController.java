package io.github.lc.oss.mc.scheduler.app.controllers.v1;

import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.servlet.ModelAndView;

import io.github.lc.oss.commons.jwt.Jwt;
import io.github.lc.oss.commons.serialization.Message;
import io.github.lc.oss.commons.serialization.Primitive;
import io.github.lc.oss.commons.serialization.Response;
import io.github.lc.oss.commons.web.tokens.CsrfTokenManager;
import io.github.lc.oss.mc.api.Messages;
import io.github.lc.oss.mc.security.Authorities;
import io.github.lc.oss.mc.scheduler.app.entity.User;
import io.github.lc.oss.mc.scheduler.app.model.Credentials;
import io.github.lc.oss.mc.scheduler.app.repository.UserRepository;
import io.github.lc.oss.mc.scheduler.app.service.IdentityService;
import io.github.lc.oss.mc.scheduler.app.service.PasswordHasher;
import io.github.lc.oss.mc.scheduler.app.validation.CredentialsValidator;
import io.github.lc.oss.mc.scheduler.security.JwtManager;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Controller
@PreAuthorize(Authorities.PUBLIC)
public class PublicController extends AbstractController {
    @Autowired
    private CredentialsValidator credentialsValidator;
    @Autowired
    private CsrfTokenManager csrfTokenManager;
    @Autowired
    private IdentityService identityService;
    @Autowired
    private JwtManager jwtManager;
    @Autowired
    private UserRepository userRepo;
    @Autowired
    private PasswordHasher passwordHasher;

    @GetMapping(path = "/login", produces = MediaType.TEXT_HTML_VALUE)
    public ModelAndView login(HttpServletRequest request, HttpServletResponse response) {
        SecurityContextHolder.clearContext();
        this.jwtManager.invalidate(request, response);
        this.csrfTokenManager.setToken(response);
        if (StringUtils.isNotBlank(this.identityService.getIdentityUrl())) {
            response.setHeader("Content-Security-Policy", "default-src 'none'; script-src 'self'; " + //
                    "connect-src 'self' " + this.identityService.getIdentityUrl()
                    + "; img-src 'self'; style-src 'self'; " + //
                    "font-src 'self'; frame-ancestors 'none';");
        }
        return new ModelAndView("views/login");
    }

    @GetMapping(path = "/error", produces = MediaType.TEXT_HTML_VALUE)
    public ModelAndView error(HttpServletRequest request, HttpServletResponse response) {
        return new ModelAndView("views/error");
    }

    @Transactional(readOnly = true)
    @PostMapping(path = "/api/v1/login", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Response<Primitive<String>>> auth(HttpServletRequest request, HttpServletResponse response,
            @RequestBody(required = false) Credentials credentials) {
        SecurityContextHolder.clearContext();

        Set<Message> messages = this.credentialsValidator.validate(credentials);
        if (!messages.isEmpty()) {
            return this.respond(messages);
        }

        User user = this.userRepo.findByUsernameIgnoreCase(credentials.getUsername());
        if (user == null) {
            return this.respond(Messages.Authentication.InvalidCredentials);
        }

        if (!this.passwordHasher.matches(credentials.getPassword(), user)) {
            return this.respond(Messages.Authentication.InvalidCredentials);
        }

        Jwt token = this.jwtManager.issueCookie(request, response, user);
        if (token == null) {
            return this.respond(Messages.Authentication.InvalidCredentials);
        }

        this.csrfTokenManager.setToken(response);

        return this.respond(new Response<>(new Primitive<>("/")));
    }

    @PostMapping(path = "/api/v1/login", consumes = MediaType.TEXT_PLAIN_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Response<Primitive<String>>> auth(HttpServletRequest request, HttpServletResponse response,
            @RequestBody String jwtValue) {
        SecurityContextHolder.clearContext();

        Jwt jwt = this.jwtManager.validate(jwtValue);
        if (jwt == null) {
            return this.respond(Messages.Authentication.InvalidCredentials);
        }

        this.csrfTokenManager.setToken(response);
        this.jwtManager.setCookie(response, jwtValue, jwt.getExpirationMillis());

        return this.respond(new Response<>(new Primitive<>("/")));
    }

    @PreAuthorize(Authorities.ANY_APP_USER)
    @DeleteMapping(path = "/api/v1/login", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Response<Primitive<String>>> logout(HttpServletRequest request,
            HttpServletResponse response) {
        this.jwtManager.invalidate(request, response);
        SecurityContextHolder.clearContext();
        return this.respond(new Response<>(new Primitive<>("/login")));
    }
}
