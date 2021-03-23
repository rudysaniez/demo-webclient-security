package webclient.security.demo;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.oauth2.client.AuthorizedClientServiceReactiveOAuth2AuthorizedClientManager;
import org.springframework.security.oauth2.client.ReactiveOAuth2AuthorizedClientManager;
import org.springframework.security.oauth2.client.ReactiveOAuth2AuthorizedClientProvider;
import org.springframework.security.oauth2.client.ReactiveOAuth2AuthorizedClientProviderBuilder;
import org.springframework.security.oauth2.client.ReactiveOAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.registration.ReactiveClientRegistrationRepository;
import org.springframework.security.oauth2.client.web.reactive.function.client.ServerOAuth2AuthorizedClientExchangeFilterFunction;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.web.reactive.function.client.WebClient;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@EnableScheduling
@EnableWebFluxSecurity
@SpringBootApplication
public class DemoClientApplication {

	public static void main(String[] args) {
		
		SpringApplication app = new SpringApplication(DemoClientApplication.class);
		app.setWebApplicationType(WebApplicationType.NONE);
		app.run(args);
	}

	@Bean
	public SecurityWebFilterChain securityWebFilterChain(ServerHttpSecurity http) {
		
		return http.
				oauth2Client().
				and().
				build();
	}
	
	@Bean
	public ReactiveOAuth2AuthorizedClientManager authorizedClientManager(ReactiveClientRegistrationRepository clientRegistrationRepository,
			ReactiveOAuth2AuthorizedClientService authorizedClientService) {
	
		ReactiveOAuth2AuthorizedClientProvider authorizedClientProvider = ReactiveOAuth2AuthorizedClientProviderBuilder.
				builder().
				clientCredentials().
				build();
		
		AuthorizedClientServiceReactiveOAuth2AuthorizedClientManager authorizedClientManager = new AuthorizedClientServiceReactiveOAuth2AuthorizedClientManager(
		            clientRegistrationRepository, authorizedClientService);
		
	    authorizedClientManager.setAuthorizedClientProvider(authorizedClientProvider);
	    
		return authorizedClientManager;
	}
	
	@Bean
	public WebClient webClient(ReactiveOAuth2AuthorizedClientManager authorizedClientManager) {
		
		ServerOAuth2AuthorizedClientExchangeFilterFunction oauth =new ServerOAuth2AuthorizedClientExchangeFilterFunction(authorizedClientManager);
		oauth.setDefaultClientRegistrationId("oidcServices");
		
		return WebClient.
				builder().
				filter(oauth).
				build();
	}
	
	@Autowired WebClient webClient;
	
	@Scheduled(fixedRate = 5000)
	public void logResourceServiceResponse() {

	    webClient.get()
	      .uri("http://localhost:8080/suppliers")
	      .retrieve()
	      .bodyToMono(String.class)
	      .map(string 
	        -> "Retrieved using Client Credentials Grant Type (webclient-security) : " + string)
	      .subscribe(log::info);
	}
}
