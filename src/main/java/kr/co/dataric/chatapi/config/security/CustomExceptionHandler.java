package kr.co.dataric.chatapi.config.security;

import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.server.ServerAuthenticationEntryPoint;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

public class CustomExceptionHandler implements ServerAuthenticationEntryPoint {
	@Override
	public Mono<Void> commence(ServerWebExchange exchange, AuthenticationException ex) {
		return null;
	}
}
