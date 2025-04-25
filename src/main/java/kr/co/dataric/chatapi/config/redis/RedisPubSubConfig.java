package kr.co.dataric.chatapi.config.redis;

import kr.co.dataric.chatapi.subscriber.ChatRedisSubscriber;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.data.redis.listener.adapter.MessageListenerAdapter;

@Configuration
@RequiredArgsConstructor
public class RedisPubSubConfig {
	
	private final ChatRedisSubscriber chatRedisSubscriber;
	
	@Bean
	public RedisMessageListenerContainer container(RedisConnectionFactory factory) {
		RedisMessageListenerContainer container = new RedisMessageListenerContainer();
		container.setConnectionFactory(factory);
		container.addMessageListener(listenerAdapter(), onlineTopic()); // 등록 되어야 함
		container.addMessageListener(listenerAdapter(), chatReadTopic()); // 읽음 업데이트도 리스닝
		return container;
	}
	
	@Bean
	public ChannelTopic onlineTopic() {
		return new ChannelTopic("onlineUpdate"); // 사용 중인 Redis Pub/Sub 채널명
	}
	
	@Bean
	public ChannelTopic chatReadTopic() {
		return new ChannelTopic("chatReadUpdate");
	}
	
	@Bean
	public MessageListenerAdapter listenerAdapter() {
		return new MessageListenerAdapter(chatRedisSubscriber, "onMessage");
	}
	
	
}
