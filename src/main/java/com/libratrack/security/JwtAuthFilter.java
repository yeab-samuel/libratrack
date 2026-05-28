package com.libratrack.security;
import com.libratrack.repository.TokenBlacklistRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import java.io.IOException;
@Component @RequiredArgsConstructor @Slf4j
public class JwtAuthFilter extends OncePerRequestFilter {
    private final JwtUtils jwtUtils;
    private final UserDetailsServiceImpl uds;
    private final TokenBlacklistRepository blacklist;
    @Override protected void doFilterInternal(HttpServletRequest req,HttpServletResponse res,FilterChain chain) throws ServletException,IOException {
        String auth=req.getHeader("Authorization");
        if(auth==null||!auth.startsWith("Bearer ")){chain.doFilter(req,res);return;}
        String token=auth.substring(7);
        try {
            String username=jwtUtils.extractUsername(token);
            if(username!=null&&SecurityContextHolder.getContext().getAuthentication()==null){
                if(!blacklist.existsByTokenJti(jwtUtils.extractJti(token))){
                    UserDetails ud=uds.loadUserByUsername(username);
                    if(jwtUtils.isTokenValid(token,ud)){
                        var at=new UsernamePasswordAuthenticationToken(ud,null,ud.getAuthorities());
                        at.setDetails(new WebAuthenticationDetailsSource().buildDetails(req));
                        SecurityContextHolder.getContext().setAuthentication(at);
                    }
                }
            }
        } catch(Exception e){log.debug("JWT error: {}",e.getMessage());}
        chain.doFilter(req,res);
    }
}
