package io.github.lc.oss.mc.scheduler.app.controllers.v1;

import java.util.HashSet;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.servlet.ModelAndView;

import io.github.lc.oss.commons.jwt.Jwt;
import io.github.lc.oss.commons.serialization.JsonMessage;
import io.github.lc.oss.commons.serialization.JsonableCollection;
import io.github.lc.oss.commons.serialization.Primitive;
import io.github.lc.oss.commons.serialization.Response;
import io.github.lc.oss.commons.web.tokens.CsrfTokenManager;
import io.github.lc.oss.mc.api.Messages;
import io.github.lc.oss.mc.scheduler.AbstractMockTest;
import io.github.lc.oss.mc.scheduler.app.entity.User;
import io.github.lc.oss.mc.scheduler.app.model.Credentials;
import io.github.lc.oss.mc.scheduler.app.repository.UserRepository;
import io.github.lc.oss.mc.scheduler.app.service.IdentityService;
import io.github.lc.oss.mc.scheduler.app.service.PasswordHasher;
import io.github.lc.oss.mc.scheduler.app.validation.CredentialsValidator;
import io.github.lc.oss.mc.scheduler.security.JwtManager;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

public class PublicControllerTest extends AbstractMockTest {
    private static class CallHelper {
        public boolean wasCalled = false;
    }

    @Mock
    private CredentialsValidator credentialsValidator;
    @Mock
    private CsrfTokenManager csrfTokenManager;
    @Mock
    private IdentityService identityService;
    @Mock
    private JwtManager jwtManager;
    @Mock
    private UserRepository userRepo;
    @Mock
    private PasswordHasher passwordHasher;

    @InjectMocks
    private PublicController controller;

    @Test
    public void test_login_withIdentityUrl() {
        HttpServletRequest request = Mockito.mock(HttpServletRequest.class);
        HttpServletResponse response = Mockito.mock(HttpServletResponse.class);

        Mockito.when(this.identityService.getIdentityUrl()).thenReturn("https://identity.local");

        Mockito.doAnswer(new Answer<Void>() {
            @Override
            public Void answer(InvocationOnMock invocation) throws Throwable {
                String value = invocation.getArgument(1);
                Assertions.assertEquals("default-src 'none'; script-src 'self'; " + //
                        "connect-src 'self' https://identity.local" + "; img-src 'self'; style-src 'self'; " + //
                        "font-src 'self'; frame-ancestors 'none';", value);
                return null;
            }
        }).when(response).setHeader(ArgumentMatchers.eq("Content-Security-Policy"), ArgumentMatchers.notNull());

        ModelAndView result = this.controller.login(request, response);
        Assertions.assertNotNull(result);
    }

    @Test
    public void test_auth_jwtError() {
        HttpServletRequest request = Mockito.mock(HttpServletRequest.class);
        HttpServletResponse response = Mockito.mock(HttpServletResponse.class);

        Credentials creds = new Credentials("junit", "pass");
        User user = Mockito.mock(User.class);

        Mockito.when(this.credentialsValidator.validate(creds)).thenReturn(new HashSet<>());
        Mockito.when(this.userRepo.findByUsernameIgnoreCase(creds.getUsername())).thenReturn(user);
        Mockito.when(this.passwordHasher.matches(creds.getPassword(), user)).thenReturn(true);
        Mockito.when(this.jwtManager.issueCookie(request, response, user)).thenReturn(null);

        ResponseEntity<Response<Primitive<String>>> result = this.controller.auth(request, response, creds);
        Assertions.assertNotNull(result);
        Assertions.assertEquals(HttpStatus.UNPROCESSABLE_ENTITY, result.getStatusCode());
        Response<Primitive<String>> body = result.getBody();
        Assertions.assertNotNull(body);
        Assertions.assertNull(body.getBody());
        JsonableCollection<JsonMessage> messages = body.getMessages();
        Assertions.assertNotNull(messages);
        Assertions.assertEquals(1, messages.size());
        JsonMessage message = messages.iterator().next();
        Assertions.assertEquals(Messages.Authentication.InvalidCredentials.getCategory(), message.getCategory());
        Assertions.assertEquals(Messages.Authentication.InvalidCredentials.getSeverity(), message.getSeverity());
        Assertions.assertEquals(Messages.Authentication.InvalidCredentials.getNumber(), message.getNumber());
    }

    @Test
    public void test_auth_external_invalid() {
        HttpServletRequest request = Mockito.mock(HttpServletRequest.class);
        HttpServletResponse response = Mockito.mock(HttpServletResponse.class);

        Mockito.when(this.jwtManager.validate("junk")).thenReturn(null);

        ResponseEntity<Response<Primitive<String>>> result = this.controller.auth(request, response, "junk");
        Assertions.assertNotNull(result);
        Assertions.assertEquals(HttpStatus.UNPROCESSABLE_ENTITY, result.getStatusCode());
        Response<Primitive<String>> body = result.getBody();
        Assertions.assertNotNull(body);
        Assertions.assertNull(body.getBody());
        JsonableCollection<JsonMessage> messages = body.getMessages();
        Assertions.assertNotNull(messages);
        Assertions.assertEquals(1, messages.size());
        JsonMessage message = messages.iterator().next();
        Assertions.assertEquals(Messages.Authentication.InvalidCredentials.getCategory(), message.getCategory());
        Assertions.assertEquals(Messages.Authentication.InvalidCredentials.getSeverity(), message.getSeverity());
        Assertions.assertEquals(Messages.Authentication.InvalidCredentials.getNumber(), message.getNumber());
    }

    @Test
    public void test_auth_external() {
        HttpServletRequest request = Mockito.mock(HttpServletRequest.class);
        HttpServletResponse response = Mockito.mock(HttpServletResponse.class);

        final CallHelper csrfHelper = new CallHelper();
        final CallHelper jwtHelper = new CallHelper();

        final Jwt jwt = new Jwt();
        jwt.setExpirationMillis(1000);

        Mockito.when(this.jwtManager.validate("valid-jwt")).thenReturn(jwt);
        Mockito.doAnswer(new Answer<Void>() {
            @Override
            public Void answer(InvocationOnMock invocation) throws Throwable {
                Assertions.assertFalse(csrfHelper.wasCalled);
                csrfHelper.wasCalled = true;
                return null;
            }
        }).when(this.csrfTokenManager).setToken(ArgumentMatchers.notNull());
        Mockito.doAnswer(new Answer<Void>() {
            @Override
            public Void answer(InvocationOnMock invocation) throws Throwable {
                Assertions.assertFalse(jwtHelper.wasCalled);
                jwtHelper.wasCalled = true;
                return null;
            }
        }).when(this.jwtManager).setCookie(ArgumentMatchers.eq(response), ArgumentMatchers.notNull(),
                ArgumentMatchers.eq(jwt.getExpirationMillis()));

        Assertions.assertFalse(csrfHelper.wasCalled);
        Assertions.assertFalse(jwtHelper.wasCalled);

        ResponseEntity<Response<Primitive<String>>> result = this.controller.auth(request, response, "valid-jwt");
        Assertions.assertNotNull(result);
        Assertions.assertEquals(HttpStatus.OK, result.getStatusCode());
        Response<Primitive<String>> body = result.getBody();
        Assertions.assertNotNull(body);
        Assertions.assertNull(body.getMessages());
        Primitive<String> payload = body.getBody();
        Assertions.assertEquals("/", payload.getValue());

        Assertions.assertTrue(csrfHelper.wasCalled);
        Assertions.assertTrue(jwtHelper.wasCalled);
    }
}
