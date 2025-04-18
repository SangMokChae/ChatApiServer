package kr.co.dataric.chatapi.config.security;

import kr.co.dataric.chatapi.filter.jwt.JwtAuthenticationFilter;
import kr.co.dataric.common.jwt.provider.JwtProvider;
import kr.co.dataric.common.redis.service.RedisService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.SecurityWebFiltersOrder;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.web.reactive.config.ResourceHandlerRegistry;
import org.springframework.web.reactive.config.WebFluxConfigurer;

import java.net.URI;

@Configuration
@EnableWebFluxSecurity
@RequiredArgsConstructor
public class SecurityConfig {
	
	private final JwtProvider jwtProvider;
	private final RedisService redisService;
	
	@Bean
	public PasswordEncoder passwordEncoder() {
		return new BCryptPasswordEncoder();
	}
	
	@Bean
	public SecurityWebFilterChain securityFilterChain(ServerHttpSecurity http) {
		return http
			.csrf(ServerHttpSecurity.CsrfSpec::disable)
			.httpBasic(ServerHttpSecurity.HttpBasicSpec::disable)
			.formLogin(ServerHttpSecurity.FormLoginSpec::disable)
			
			.authorizeExchange(exchange -> exchange
				.pathMatchers(
					"/", "/favicon.ico",
					"/css/**", "/js/**", "/img/**",
					"/login", "/logout", "/loginProc",
					"/ws/chat/**",
					"/ws/read/**",
					"/ws/notify/**",
					"/ws/status/**"
				).permitAll()
				.anyExchange().authenticated()
			)
			.addFilterAt(new JwtAuthenticationFilter(jwtProvider, redisService), SecurityWebFiltersOrder.AUTHENTICATION)
			// JwtAuthenticationFilter에서 WebFilter를 상속하고 있기 때문에 수동 설정을 할 필요가 없다.
			
			// ✅ 예외 처리
			.exceptionHandling(ex -> ex
				.authenticationEntryPoint((exchange, ex1) -> {
					if (!exchange.getResponse().isCommitted()) {
						exchange.getResponse().setStatusCode(HttpStatus.SEE_OTHER); // 303 See Other
						exchange.getResponse().getHeaders().setLocation(URI.create("/login"));
					}
					return exchange.getResponse().setComplete();
				})
				.accessDeniedHandler((exchange, denied) -> {
					if (!exchange.getResponse().isCommitted()) {
						exchange.getResponse().setStatusCode(HttpStatus.SEE_OTHER); // 303
						exchange.getResponse().getHeaders().setLocation(URI.create("/error?code=denied"));
					}
					return exchange.getResponse().setComplete();
				})
			)
			
			.build();
	}
	
	@Bean
	public WebFluxConfigurer webFluxConfigurer() {
		return new WebFluxConfigurer() {
			@Override
			public void addResourceHandlers(ResourceHandlerRegistry registry) {
				registry.addResourceHandler("/favicon.ico")
					.addResourceLocations("classpath:/static/img/puppy.ico");
			}
		};
	}
}