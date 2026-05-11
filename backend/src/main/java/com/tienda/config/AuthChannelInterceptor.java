package com.tienda.config;

import com.tienda.security.JwtUtil;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Collections;
import java.util.List;

public class AuthChannelInterceptor implements ChannelInterceptor {

    private final JwtUtil jwtUtil;

    public AuthChannelInterceptor(JwtUtil jwtUtil) {
        this.jwtUtil = jwtUtil;
    }

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);

        if (accessor != null && StompCommand.CONNECT.equals(accessor.getCommand())) {
            List<String> authHeaders = accessor.getNativeHeader("Authorization");
            if (authHeaders != null && !authHeaders.isEmpty()) {
                String bearer = authHeaders.get(0);
                if (bearer.startsWith("Bearer ")) {
                    String token = bearer.substring(7);
                    if (jwtUtil.validateToken(token) && !jwtUtil.isRefreshToken(token)) {
                        String email = jwtUtil.getEmailFromToken(token);
                        String role = jwtUtil.getRoleFromToken(token);

                        UsernamePasswordAuthenticationToken auth =
                                new UsernamePasswordAuthenticationToken(
                                        email, null,
                                        Collections.singletonList(new SimpleGrantedAuthority(role))
                                );
                        accessor.setUser(auth);
                    }
                }
            }
        }

        return message;
    }
}
