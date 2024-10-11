package io.github.lc.oss.mc.scheduler.app.aop;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.ModelAndView;

import io.github.lc.oss.commons.l10n.Variable;
import io.github.lc.oss.commons.web.advice.AbstractCommonAdviceMvCustomizer;
import io.github.lc.oss.mc.scheduler.app.service.IdentityService;
import io.github.lc.oss.mc.scheduler.app.service.NodeService;
import io.github.lc.oss.mc.scheduler.security.WebUser;

@Component
public class CommonAdviceMvCustomizer extends AbstractCommonAdviceMvCustomizer {
    @Autowired
    private IdentityService identityService;
    @Autowired
    private NodeService nodeService;

    @Override
    public ModelAndView customize(ModelAndView mv) {
        if (mv == null) {
            return mv;
        }

        WebUser user = this.getCurrentUserInfo();
        if (user != null) {
            mv.addObject("selfIssuing", this.identityService.isSelfIssuing());
            mv.addObject("showMenu", true);
            mv.addObject("urlPrefix", this.nodeService.getUrlPrefix());
            mv.addObject("headerUser",
                    this.getText("application.header.user", new Variable("DisplayName", user.getDisplayName())));
            mv.addObject("displayName", user.getDisplayName());
        }
        return mv;
    }

    protected WebUser getCurrentUserInfo() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof WebUser) {
            return (WebUser) auth.getPrincipal();
        }
        return null;
    }
}
