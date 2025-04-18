package kr.co.dataric.chatapi.kafka.producer;

import kr.co.dataric.common.dto.ReadReceiptEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.reactivestreams.Publisher;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;

@Slf4j
@Component
@RequiredArgsConstructor
public class ReadEventProducer {
	
	private final KafkaTemplate<String, Object> kafkaTemplate;
	private static final String TOPIC = "chat.read.receipt";
	
	public Mono<Void> sendReadEvent(ReadReceiptEvent event) {
		return Mono.fromFuture(() -> {
				try {
					ProducerRecord<String, Object> record = new ProducerRecord<>(TOPIC, event);
					record.headers().add("__TypeId__", ReadReceiptEvent.class.getName().getBytes(StandardCharsets.UTF_8));
					return kafkaTemplate.send(record);
				} catch (Exception e) {
					log.error("Kafka 전송 실패", e);
					throw new RuntimeException("Kafka send error", e);
				}
			}).doOnSuccess(result -> log.info("✅ Kafka 읽음 이벤트 전송 성공: {}", event))
			.doOnError(error -> log.error("❌ Kafka 읽음 이벤트 전송 실패", error))
			.then(); // Mono<Void>
	}
}
