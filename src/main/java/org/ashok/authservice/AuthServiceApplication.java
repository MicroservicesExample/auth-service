package org.ashok.authservice;

import java.util.Set;
import java.util.UUID;

import javax.sql.DataSource;

import org.ashok.authservice.client.ClientRegistrationProperties;
import org.ashok.authservice.user.RegisteredUsers;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcOperations;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;
import org.springframework.security.oauth2.core.oidc.OidcScopes;
import org.springframework.security.oauth2.server.authorization.JdbcOAuth2AuthorizationConsentService;
import org.springframework.security.oauth2.server.authorization.JdbcOAuth2AuthorizationService;
import org.springframework.security.oauth2.server.authorization.OAuth2AuthorizationConsentService;
import org.springframework.security.oauth2.server.authorization.OAuth2AuthorizationService;
import org.springframework.security.oauth2.server.authorization.client.JdbcRegisteredClientRepository;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClientRepository;
import org.springframework.security.provisioning.JdbcUserDetailsManager;
import org.springframework.security.provisioning.UserDetailsManager;

@SpringBootApplication
public class AuthServiceApplication {

	public static void main(String[] args) {
		SpringApplication.run(AuthServiceApplication.class, args);
	}
	
	@Bean
    PasswordEncoder passwordEncoder() {
        return PasswordEncoderFactories.createDelegatingPasswordEncoder();
    }
	
}

@Configuration
class UsersConfiguration {

    @Bean
    JdbcUserDetailsManager jdbcUserDetailsManager(DataSource dataSource) {
        return new JdbcUserDetailsManager(dataSource);
    }

    @Bean
    ApplicationRunner usersRunner(UserDetailsManager userDetailsManager, RegisteredUsers users) {
        return args -> {
            var builder = User.builder();
			
            users.getUsers().forEach((registeredUser) -> {
                if (!userDetailsManager.userExists(registeredUser.getUsername())) {
                   
                	var user = builder.roles(registeredUser.getRoles().toArray(new String[0]))
                            .username(registeredUser.getUsername())
                            .password(registeredUser.getPassword())
                            .build();
                    userDetailsManager.createUser(user);
                }
            });
        };
    }

}

/*
 * This is hard coded client App configuration in the db.
 * TODO: You should have an interface to configure the client apps
 */

@Configuration
class ClientsConfiguration {

    	
	@Bean
    RegisteredClientRepository registeredClientRepository(JdbcTemplate template) {
        return new JdbcRegisteredClientRepository(template);
    }

    @Bean
    ApplicationRunner clientsRunner(RegisteredClientRepository repository, ClientRegistrationProperties clientProperties) {
        return args -> {
            
            if (repository.findByClientId(clientProperties.getClientId()) == null) {
                repository.save(
                        RegisteredClient
                                .withId(UUID.randomUUID().toString())
                                .clientId(clientProperties.getClientId())
                                .clientSecret(clientProperties.getClientSecret())
                                .clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_BASIC)
                                .authorizationGrantTypes(grantTypes -> grantTypes.addAll(Set.of(
                                        AuthorizationGrantType.CLIENT_CREDENTIALS,
                                        AuthorizationGrantType.AUTHORIZATION_CODE,
                                        AuthorizationGrantType.REFRESH_TOKEN)))
                                .redirectUri(clientProperties.getRedirectUri())
                                .scopes(scopes -> scopes.addAll(Set.of("user.read", "user.write", OidcScopes.OPENID)))
                                .build()
                );
            }
        };
    }
}

@Configuration
class AuthorizationConfiguration {

    @Bean
    OAuth2AuthorizationConsentService jdbcOAuth2AuthorizationConsentService(
            JdbcOperations jdbcOperations, RegisteredClientRepository repository) {
        return new JdbcOAuth2AuthorizationConsentService(jdbcOperations, repository);
    }

    @Bean
    OAuth2AuthorizationService jdbcOAuth2AuthorizationService(
            JdbcOperations jdbcOperations, RegisteredClientRepository rcr) {
        return new JdbcOAuth2AuthorizationService(jdbcOperations, rcr);
    }
}


