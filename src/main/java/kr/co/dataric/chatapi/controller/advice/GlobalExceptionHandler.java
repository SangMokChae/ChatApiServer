package kr.co.dataric.chatapi.controller.advice;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {
	
	@ExceptionHandler(RuntimeException.class)
	public Mono<Void> handleRuntimeException(RuntimeException ex, ServerWebExchange exchange) {
		if (exchange.getRequest().getURI().getPath().startsWith("/ws/")) {
			log.warn("❌ WebSocket 요청에서 예외 발생 - 응답 생략");
			return Mono.empty();
		}
		log.error("❗ 예외 처리", ex);
		exchange.getResponse().setStatusCode(HttpStatus.INTERNAL_SERVER_ERROR);
		return exchange.getResponse().setComplete();
	}
}
