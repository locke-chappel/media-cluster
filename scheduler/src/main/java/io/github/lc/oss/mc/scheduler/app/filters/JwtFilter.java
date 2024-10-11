package io.github.lc.oss.mc.scheduler.app.filters;

import java.io.IOException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.web.filter.OncePerRequestFilter;

import io.github.lc.oss.mc.scheduler.security.JwtManager;
import io.github.lc.oss.mc.scheduler.security.WebUser;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

public class JwtFilter extends OncePerRequestFilter {
    @Autowired
    private JwtManager jwtManager;

    @Override
    protected void doFilterInternal( //
            HttpServletRequest request, //
            HttpServletResponse response, //
            FilterChain chain) //
            throws ServletException, IOException {
        WebUser user = this.jwtManager.validate(request, response);
        if (user == null) {
            chain.doFilter(request, response);
            return;
        }

        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(user, null,
                user.getAuthorities());

        auth.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

        SecurityContextHolder.getContext().setAuthentication(auth);
        chain.doFilter(request, response);
    }
}
