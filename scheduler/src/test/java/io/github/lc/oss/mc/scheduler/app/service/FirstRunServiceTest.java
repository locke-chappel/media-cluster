package io.github.lc.oss.mc.scheduler.app.service;

import org.mockito.InjectMocks;
import org.mockito.Mock;

import io.github.lc.oss.commons.l10n.L10N;
import io.github.lc.oss.mc.scheduler.AbstractMockTest;
import io.github.lc.oss.mc.scheduler.app.repository.NodeRepository;
import io.github.lc.oss.mc.scheduler.app.repository.UserRepository;
import io.github.lc.oss.mc.scheduler.security.SecureConfig;

public class FirstRunServiceTest extends AbstractMockTest {
    @Mock
    private L10N l10n;
    @Mock
    private NodeRepository nodeRepo;
    @Mock
    private PasswordHasher passwordHasher;
    @Mock
    private SecureConfig secureConfig;
    @Mock
    private SignatureService signatureService;
    @Mock
    private UserRepository userRepo;

    @InjectMocks
    private FirstRunService service;
}
