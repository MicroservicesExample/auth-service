package org.ashok.authservice.token;

import java.util.Set;
import java.util.stream.Collectors;

import org.ashok.authservice.keys.RsaKeyPairRepository;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.oauth2.server.authorization.OAuth2TokenType;
import org.springframework.security.oauth2.server.authorization.token.JwtEncodingContext;
import org.springframework.security.oauth2.server.authorization.token.OAuth2TokenCustomizer;
import java.util.Collections;
@Configuration
public class TokenConfig {
	
	private final RsaKeyPairRepository keyPairRepository;
	
	TokenConfig(RsaKeyPairRepository keyPairRepository){
		this.keyPairRepository = keyPairRepository;
	}
	
	/**
	 * Adds roles as custom claims in access token
	 * adds key id in token header
	 * @return
	 */
	
	@Bean
	OAuth2TokenCustomizer<JwtEncodingContext> jwtTokenCustomizer(){
		
		return (context) -> {
			if(OAuth2TokenType.ACCESS_TOKEN.equals(context.getTokenType())) {
				context.getClaims().claims(claims -> {
					Set<String> roles = AuthorityUtils.authorityListToSet(context.getPrincipal().getAuthorities())
											.stream()
											.map(authority -> authority.replaceFirst("^ROLE_", ""))
											.collect(Collectors.collectingAndThen(Collectors.toSet(), Collections::unmodifiableSet));
					claims.put("roles", roles);
					
				});
			}
			
			addKeyId(context);
			
		};
		
	}
	
	private void addKeyId(JwtEncodingContext context) {
		var keyPairs = this.keyPairRepository.findKeyPairs();
        var randomKey = (int) Math.random() * keyPairs.size();
        var kid = keyPairs.get(randomKey).id();
        context.getJwsHeader().keyId(kid);
	}
}
