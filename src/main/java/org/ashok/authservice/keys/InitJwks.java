package org.ashok.authservice.keys;

import java.time.Instant;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class InitJwks {

	private static final Logger logger = LoggerFactory.getLogger(InitJwks.class);
	
	@Bean
	ApplicationRunner jwkRunner(KeyGenerator generator, RsaKeyPairRepository repository) {
		return args -> {
			
			for(int i =0; i < 2; i++) {//generate 2 keys
				String keyId = UUID.randomUUID().toString();
				logger.info("Generating key with id {}", keyId);
				repository.save(generator.generateKeyPair(keyId, Instant.now()));
			}
			
		};
	}
}
