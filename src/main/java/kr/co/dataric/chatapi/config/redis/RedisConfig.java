package kr.co.dataric.chatapi.config.redis;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import kr.co.dataric.chatapi.repository.room.ChatRoomLastReadRepository;
import kr.co.dataric.chatapi.service.impl.ChatRoomLastReadService;
import kr.co.dataric.common.dto.ChatRoomRedisDto;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.ReactiveRedisConnectionFactory;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import reactor.core.publisher.Mono;

@Configuration
public class RedisConfig {
	
	@Bean
	public ReactiveRedisTemplate<String, ChatRoomRedisDto> chatRoomRedisTemplate(ReactiveRedisConnectionFactory factory) {
		ObjectMapper objectMapper = new ObjectMapper();
		objectMapper.registerModule(new JavaTimeModule());
		objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
		
		Jackson2JsonRedisSerializer<ChatRoomRedisDto> valueSerializer = new Jackson2JsonRedisSerializer<>(objectMapper, ChatRoomRedisDto.class);
		StringRedisSerializer keySerializer = new StringRedisSerializer();
		
		RedisSerializationContext.SerializationPair<String> keyPair = RedisSerializationContext.SerializationPair.fromSerializer(keySerializer);
		RedisSerializationContext.SerializationPair<ChatRoomRedisDto> valuePair = RedisSerializationContext.SerializationPair.fromSerializer(valueSerializer);
		
		RedisSerializationContext<String, ChatRoomRedisDto> context = RedisSerializationContext
			.<String, ChatRoomRedisDto>newSerializationContext(keyPair)
			.value(valuePair)
			.build();
		
		return new ReactiveRedisTemplate<>(factory, context);
	}
	
	@Bean
	public ReactiveRedisTemplate<String, Object> reactiveRedisTemplate(ReactiveRedisConnectionFactory factory) {
		ObjectMapper objectMapper = new ObjectMapper();
		objectMapper.registerModule(new JavaTimeModule());
		objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
		
		Jackson2JsonRedisSerializer<Object> valueSerializer = new Jackson2JsonRedisSerializer<>(objectMapper, Object.class);
		StringRedisSerializer keySerializer = new StringRedisSerializer();
		
		RedisSerializationContext.SerializationPair<String> keyPair = RedisSerializationContext.SerializationPair.fromSerializer(keySerializer);
		RedisSerializationContext.SerializationPair<Object> valuePair = RedisSerializationContext.SerializationPair.fromSerializer(valueSerializer);
		
		RedisSerializationContext<String, Object> context = RedisSerializationContext
			.<String, Object>newSerializationContext(keyPair)
			.value(valuePair)
			.build();
		
		return new ReactiveRedisTemplate<>(factory, context);
	}
	
	@Bean
	public ObjectMapper redisObjectMapper() {
		ObjectMapper objectMapper = new ObjectMapper();
		objectMapper.registerModule(new JavaTimeModule());
		objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
		return objectMapper;
	}
	
	@Bean(name = "defaultLastReadMessageId")
	public Mono<String> defaultLastReadMessageId() {
		return Mono.just("INITIAL");
	}
	
}
