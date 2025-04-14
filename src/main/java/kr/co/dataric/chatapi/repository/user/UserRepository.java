package kr.co.dataric.chatapi.repository.user;

import kr.co.dataric.chatapi.entity.user.User;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Mono;

public interface UserRepository extends ReactiveCrudRepository<User, Long> {
	Mono<User> findByUsername(String username);
}
