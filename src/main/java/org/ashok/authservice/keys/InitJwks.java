package org.ashok.authservice.keys;

import java.time.Instant;
import java.util.UUID;

import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class InitJwks {

	@Bean
	ApplicationRunner jwkRunner(KeyGenerator generator, RsaKeyPairRepository repository) {
		return args -> {
			
			for(int i =0; i < 2; i++) {//generate 2 keys
				String keyId = UUID.randomUUID().toString();
				repository.save(generator.generateKeyPair(keyId, Instant.now()));
			}
			
		};
	}
}
