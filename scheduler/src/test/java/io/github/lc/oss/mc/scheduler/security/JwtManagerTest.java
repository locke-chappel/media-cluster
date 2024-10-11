package io.github.lc.oss.mc.scheduler.security;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.security.KeyPair;
import java.time.Clock;
import java.time.Instant;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import io.github.lc.oss.commons.identity.model.ApplicationInfo;
import io.github.lc.oss.commons.jwt.Jwt;
import io.github.lc.oss.commons.jwt.JwtHeader;
import io.github.lc.oss.commons.jwt.UserCache;
import io.github.lc.oss.commons.signing.Algorithms;
import io.github.lc.oss.mc.scheduler.AbstractMockTest;
import io.github.lc.oss.mc.scheduler.app.service.IdentityService;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

public class JwtManagerTest extends AbstractMockTest {
    private static class CallHelper {
        public boolean wasCalled = false;
    }

    @Mock
    private Clock clock;
    @Mock
    private IdentityService identityService;
    @Mock
    private SecureConfig secureConfig;
    @Mock
    private UserCache<WebUser> userCache;

    @InjectMocks
    private JwtManager manager;

    @BeforeEach
    public void setup() {
        this.setField("secrets", null, this.manager);
        this.setField("sessionMaxAge", 30000, this.manager);
        this.setField("cookieName", "junit", this.manager);
        this.setField("cookieDomain", "junit.home", this.manager);
    }

    @Test
    public void test_cleanCache() {
        this.manager.cleanCache();
    }

    @Test
    public void test_log() {
        this.manager.log("Message", null);

        this.manager.log("Message", new RuntimeException());
    }

    @Test
    public void test_getSignSecret_noId() {
        final byte[] def = new byte[] { 0x00 };
        JwtHeader header = new JwtHeader();

        header.setKeyId(null);
        byte[] result = this.manager.getSignSecret(header, def);
        Assertions.assertNull(result);

        header.setKeyId("");
        result = this.manager.getSignSecret(header, def);
        Assertions.assertNull(result);

        header.setKeyId(" \t \r \n \t ");
        result = this.manager.getSignSecret(header, def);
        Assertions.assertNull(result);
    }

    @Test
    public void test_getSignSecret_keyNotFound() {
        JwtManager manager = new JwtManager();
        Map<String, KeyPair> secrets = new HashMap<>();
        this.setField("secrets", secrets, manager);

        final byte[] def = new byte[] { 0x00 };
        JwtHeader header = new JwtHeader();
        header.setKeyId("key-id");

        byte[] result = manager.getSignSecret(header, def);
        Assertions.assertNull(result);
    }

    @Test
    public void test_getValidateSecret_noId() {
        JwtHeader header = new JwtHeader();
        byte[] def = new byte[] { 0x00 };

        byte[] result = this.manager.getValidateSecret(header, def);
        Assertions.assertNull(result);
    }

    @Test
    public void test_getValidateSecret_unknownId() {
        JwtHeader header = new JwtHeader();
        header.setKeyId("id");
        byte[] def = new byte[] { 0x00 };

        Map<String, KeyPair> secrets = new HashMap<>();

        this.setField("secrets", secrets, this.manager);

        byte[] result = this.manager.getValidateSecret(header, def);
        Assertions.assertNull(result);
    }

    @Test
    public void test_getValidateSecret_noPath() {
        JwtManager manager = new JwtManager();

        JwtHeader header = new JwtHeader();
        header.setKeyId("id");

        this.setField("secrets", null, manager);
        this.setField("keyStorePath", null, manager);

        try {
            manager.getValidateSecret(header, null);
            Assertions.fail("Expected exception");
        } catch (RuntimeException ex) {
            Assertions.assertEquals("Key store path cannot be blank.", ex.getMessage());
        }
    }

    @Test
    public void test_loadKey_exception() {
        this.setField("keyStorePath", "junk", this.manager);

        JwtHeader header = new JwtHeader();
        header.setKeyId("id");

        Mockito.when(this.secureConfig.getKeystorePassword()).thenReturn("password");
        Mockito.when(this.secureConfig.getUserJwtSecrets()).thenReturn(Arrays.asList(header.getKeyId()));

        try {
            this.manager.getValidateSecret(header, null);
            Assertions.fail("Expected exception");
        } catch (RuntimeException ex) {
            Assertions.assertEquals("Error loading KeyPair", ex.getMessage());
        }
    }

    @Test
    public void test_isAlgorithmAllowed() {
        boolean result = this.manager.isAlgorithmAllowed(null);
        Assertions.assertFalse(result);

        result = this.manager.isAlgorithmAllowed(Algorithms.HS512);
        Assertions.assertFalse(result);

        result = this.manager.isAlgorithmAllowed(Algorithms.ED25519);
        Assertions.assertTrue(result);
    }

    @Test
    public void test_refreshCleanup() {
        String subject = "sub";

        Set<String> inProgressRefresh = this.getField("inProgressRefresh", this.manager);
        inProgressRefresh.add(subject);

        JwtManager.RefreshCleanup task = this.manager.newRefreshCleanup(subject);

        Assertions.assertTrue(inProgressRefresh.contains(subject));
        task.run();
        Assertions.assertFalse(inProgressRefresh.contains(subject));
    }

    @Test
    public void test_validate_refresh() {
        final Instant now = Instant.now();
        final ApplicationInfo appInfo = new ApplicationInfo();
        appInfo.setSessionMax(10000);
        appInfo.setSessionTimeout(10000);

        final WebUser wu = Mockito.mock(WebUser.class);

        HttpServletRequest request = Mockito.mock(HttpServletRequest.class);
        HttpServletResponse response = Mockito.mock(HttpServletResponse.class);

        Mockito.when(this.clock.instant()).thenReturn(now);
        Mockito.when(this.identityService.getApplicationInfo()).thenReturn(appInfo);
        Mockito.when(this.identityService.refreshSession(ArgumentMatchers.any())).thenReturn("new-jwt");
        Mockito.when(this.userCache.get(ArgumentMatchers.any())).thenReturn(wu);

        JwtManager manager = new JwtManager() {
            @Override
            public Jwt validate(String encoded) {
                Jwt jwt = new Jwt();
                jwt.setExpirationMillis(now.toEpochMilli() + 1000);
                return jwt;
            }
        };
        this.setField("clock", this.clock, manager);
        this.setField("identityService", this.identityService, manager);
        this.setField("cookieName", "junit", manager);
        this.setField("cookieDomain", "junit.home", manager);
        this.setField("userCache", this.userCache, manager);

        WebUser result = manager.validate(request, response);
        Assertions.assertSame(wu, result);
    }

    @Test
    public void test_validate_refreshFail() {
        final Instant now = Instant.now();
        final ApplicationInfo appInfo = new ApplicationInfo();
        appInfo.setSessionMax(10000);
        appInfo.setSessionTimeout(10000);

        HttpServletRequest request = Mockito.mock(HttpServletRequest.class);
        HttpServletResponse response = Mockito.mock(HttpServletResponse.class);

        Mockito.when(this.clock.instant()).thenReturn(now);
        Mockito.when(this.identityService.getApplicationInfo()).thenReturn(appInfo);
        Mockito.when(this.identityService.refreshSession(ArgumentMatchers.any())).thenReturn(null);
        Mockito.when(request.getCookies())
                .thenReturn(new Cookie[] { new Cookie("other-cookie", "value"), new Cookie("junit", "value2") });

        JwtManager manager = new JwtManager() {
            @Override
            public Jwt validate(String encoded) {
                if (encoded == null) {
                    return null;
                }
                Jwt jwt = new Jwt();
                jwt.setExpirationMillis(now.toEpochMilli() + 1000);
                return jwt;
            }
        };
        this.setField("clock", this.clock, manager);
        this.setField("identityService", this.identityService, manager);
        this.setField("cookieName", "junit", manager);
        this.setField("cookieDomain", "junit.home", manager);
        this.setField("userCache", this.userCache, manager);

        WebUser result = manager.validate(request, response);
        Assertions.assertNull(result);
    }

    @Test
    public void test_validate_refreshInProgress() {
        final Instant now = Instant.now();
        final ApplicationInfo appInfo = new ApplicationInfo();
        appInfo.setSessionMax(10000);
        appInfo.setSessionTimeout(10000);

        final WebUser wu = Mockito.mock(WebUser.class);

        HttpServletRequest request = Mockito.mock(HttpServletRequest.class);
        HttpServletResponse response = Mockito.mock(HttpServletResponse.class);

        Mockito.when(this.clock.instant()).thenReturn(now);
        Mockito.when(this.identityService.getApplicationInfo()).thenReturn(appInfo);
        Mockito.when(this.userCache.get(ArgumentMatchers.any())).thenReturn(wu);
        Mockito.verify(this.identityService, Mockito.never()).refreshSession(ArgumentMatchers.any());

        JwtManager manager = new JwtManager() {
            @Override
            public Jwt validate(String encoded) {
                Jwt jwt = new Jwt();
                jwt.setExpirationMillis(now.toEpochMilli() + 1000);
                return jwt;
            }
        };
        this.setField("clock", this.clock, manager);
        this.setField("identityService", this.identityService, manager);
        this.setField("cookieName", "junit", manager);
        this.setField("cookieDomain", "junit.home", manager);
        this.setField("userCache", this.userCache, manager);

        Set<String> inProgressRefresh = this.getField("inProgressRefresh", manager);
        inProgressRefresh.add(null);

        WebUser result = manager.validate(request, response);
        Assertions.assertSame(wu, result);
    }

    @Test
    public void test_setCookie_negativeExpiration() {
        HttpServletResponse response = Mockito.mock(HttpServletResponse.class);

        Mockito.when(this.clock.instant()).thenReturn(Instant.now());
        Mockito.doAnswer(new Answer<Void>() {
            @Override
            public Void answer(InvocationOnMock invocation) throws Throwable {
                Cookie c = invocation.getArgument(0);
                Assertions.assertNotNull(c);
                Assertions.assertEquals(0, c.getMaxAge());
                return null;
            }
        }).when(response).addCookie(ArgumentMatchers.notNull());

        this.manager.setCookie(response, "token", (long) -1);
    }

    @Test
    public void test_externalIdm_getSessionTimeout() {
        JwtManager manager = new JwtManager();
        this.setField("sessionTimeout", -1, manager);
        this.setField("identityService", this.identityService, manager);

        final ApplicationInfo appInfo = new ApplicationInfo();
        appInfo.setSessionMax(10000);
        appInfo.setSessionTimeout(10000);

        Mockito.when(this.identityService.getApplicationInfo()).thenReturn(appInfo);

        Method m = this.findMethod("getSessionTimeout", JwtManager.class);
        m.setAccessible(true);
        long result = -1;
        try {
            result = (long) m.invoke(manager);
        } catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException ex) {
            Assertions.fail("Unexpected exception");
        }
        Assertions.assertEquals(appInfo.getSessionTimeout(), result);
    }

    @Test
    public void test_externalIdm_getSessionMaxAge() {
        JwtManager manager = new JwtManager();
        this.setField("sessionTimeout", -1, manager);
        this.setField("identityService", this.identityService, manager);

        final ApplicationInfo appInfo = new ApplicationInfo();
        appInfo.setSessionMax(10000);
        appInfo.setSessionTimeout(10000);

        Mockito.when(this.identityService.getApplicationInfo()).thenReturn(appInfo);

        Method m = this.findMethod("getSessionMaxAge", JwtManager.class);
        m.setAccessible(true);
        long result = -1;
        try {
            result = (long) m.invoke(manager);
        } catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException ex) {
            Assertions.fail("Unexpected exception");
        }
        Assertions.assertEquals(appInfo.getSessionTimeout(), result);
    }

    @Test
    public void test_invalidate_noToken() {
        HttpServletRequest request = Mockito.mock(HttpServletRequest.class);
        HttpServletResponse response = Mockito.mock(HttpServletResponse.class);

        final CallHelper addCookie = new CallHelper();

        Mockito.when(request.getCookies()).thenReturn(new Cookie[0]);
        Mockito.doAnswer(new Answer<Void>() {
            @Override
            public Void answer(InvocationOnMock invocation) throws Throwable {
                Cookie cookie = invocation.getArgument(0);
                Assertions.assertEquals("junit", cookie.getName());
                Assertions.assertEquals(0, cookie.getMaxAge());
                Assertions.assertFalse(addCookie.wasCalled);
                addCookie.wasCalled = true;
                return null;
            }
        }).when(response).addCookie(ArgumentMatchers.notNull());

        Assertions.assertFalse(addCookie.wasCalled);
        this.manager.invalidate(request, response);
        Assertions.assertTrue(addCookie.wasCalled);
    }

    @Test
    public void test_invalidate_blankToken() {
        HttpServletRequest request = Mockito.mock(HttpServletRequest.class);
        HttpServletResponse response = Mockito.mock(HttpServletResponse.class);

        final Cookie c = new Cookie("junit", "");
        final CallHelper addCookie = new CallHelper();

        Mockito.when(request.getCookies()).thenReturn(new Cookie[] { c });
        Mockito.doAnswer(new Answer<Void>() {
            @Override
            public Void answer(InvocationOnMock invocation) throws Throwable {
                Cookie cookie = invocation.getArgument(0);
                Assertions.assertEquals("junit", cookie.getName());
                Assertions.assertEquals(0, cookie.getMaxAge());
                Assertions.assertFalse(addCookie.wasCalled);
                addCookie.wasCalled = true;
                return null;
            }
        }).when(response).addCookie(ArgumentMatchers.notNull());

        Assertions.assertFalse(addCookie.wasCalled);
        this.manager.invalidate(request, response);
        Assertions.assertTrue(addCookie.wasCalled);
    }

    @Test
    public void test_invalidate_invalidButFormatted() {
        HttpServletRequest request = Mockito.mock(HttpServletRequest.class);
        HttpServletResponse response = Mockito.mock(HttpServletResponse.class);

        final Cookie c = new Cookie("junit", "h.b.sig");
        final CallHelper addCookie = new CallHelper();

        Mockito.when(request.getCookies()).thenReturn(new Cookie[] { c });
        Mockito.doAnswer(new Answer<Void>() {
            @Override
            public Void answer(InvocationOnMock invocation) throws Throwable {
                Cookie cookie = invocation.getArgument(0);
                Assertions.assertEquals("junit", cookie.getName());
                Assertions.assertEquals(0, cookie.getMaxAge());
                Assertions.assertFalse(addCookie.wasCalled);
                addCookie.wasCalled = true;
                return null;
            }
        }).when(response).addCookie(ArgumentMatchers.notNull());

        Assertions.assertFalse(addCookie.wasCalled);
        this.manager.invalidate(request, response);
        Assertions.assertTrue(addCookie.wasCalled);
    }

    @Test
    public void test_invalidate_invalidToken_noResponse() {
        HttpServletRequest request = Mockito.mock(HttpServletRequest.class);

        final Cookie c = new Cookie("junit", "ew0KICAiYWxnIjogIkVEMjU1MTkiLA0KICAidHlwIjogIkpXVCINCn0.e30.sig");

        Mockito.when(request.getCookies()).thenReturn(new Cookie[] { c });

        this.manager.invalidate(request, null);
    }
}
