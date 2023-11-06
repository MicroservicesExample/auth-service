package org.ashok.authservice;

import java.util.Map;
import java.util.Set;
import java.util.UUID;

import javax.sql.DataSource;

import org.ashok.authservice.client.ClientRegistrationProperties;
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
    ApplicationRunner usersRunner(UserDetailsManager userDetailsManager) {
        return args -> {
            var builder = User.builder();
            var users = Map.of(
                    "ashok", "{bcrypt}$2a$10$oxqx/bdaY9eMSceKwVjsA.bEyaFbdPq/NXk2xbx9sdp9yF4HQIHcG",
                    "admin", "{bcrypt}$2a$10$iSRhqIhA95xls5HbYFKFyOxcUuiIOEQUW4MBJHO.LiSQlzUvA3x/W");
            users.forEach((username, password) -> {
                if (!userDetailsManager.userExists(username)) {
                    String role = "admin".equalsIgnoreCase(username) ? "ADMIN" : "USER";
                	var user = builder.roles(role)
                            .username(username)
                            .password(password)
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


