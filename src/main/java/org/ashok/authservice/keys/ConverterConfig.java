package org.ashok.authservice.keys;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;
import org.springframework.security.crypto.encrypt.Encryptors;
import org.springframework.security.crypto.encrypt.TextEncryptor;

@Configuration
@PropertySource(value={"classpath:application.yml"})
class ConverterConfig {
	
	  @Value(value = "${jwk.persistence.encrypt.password}")
	  String password;
	  
	  @Value(value = "${jwk.persistence.encrypt.salt}")
	  String salt;
	
	  @Bean 
	  TextEncryptor textEncryptor() 
	  { 
		  return Encryptors.text(password, salt); 
	  }
	 
			 
	
	@Bean
    RsaPublicKeyConverter rsaPublicKeyConverter(TextEncryptor textEncryptor) {
        return new RsaPublicKeyConverter(textEncryptor);
    }

    @Bean
    RsaPrivateKeyConverter rsaPrivateKeyConverter(TextEncryptor textEncryptor) {
        return new RsaPrivateKeyConverter(textEncryptor);
    }
}
