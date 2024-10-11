package io.github.lc.oss.mc.worker.app.controllers.v1;

import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;

import io.github.lc.oss.mc.security.Authorities;

@Controller
@PreAuthorize(Authorities.PUBLIC)
public class ResourceController extends io.github.lc.oss.commons.web.controllers.ResourceController {
    private static final Set<String> ALLOWED_PAGED = Collections.unmodifiableSet(new HashSet<>(Arrays.asList( //
            "status" //
    )));

    @Override
    protected Set<String> getAllowedPages() {
        return ResourceController.ALLOWED_PAGED;
    }

    @Override
    protected boolean isPageAllowed(Path path) {
        return true;
    }
}
