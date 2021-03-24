WebClient ServerOAuth2AuthorizedClientExchangeFilterFunction
============================================================

Presentation
------------

Using WebClient with a Server OAuth2 Authorized client filter.
In the application.yml, the configuration is setted :

	spring:
	  security:
	    oauth2:
	      client:
	        registration:
	          oidcServices:
	            authorization-grant-type: client_credentials
	            client-id: ${CLIENT_ID}
	            client-secret: ${CLIENT_SECRET}
	            scope:
	            - openid
	        provider:
	          oidcServices:
	            token-uri: ${TOKEN_URI}
	            
This part will be used by the ServerOAuth2AuthorizedClientExchangeFilterFunction in the **WebClient.builder().filter(oauth).build()**.

Note : Remember this name **oidcServices** for the registration.

@Bean ReactiveOAuth2AuthorizedClientManager
---------------------------------------------

This component use **ReactiveClientRegistrationRepository** and **ReactiveOAuth2AuthorizedClientService**.


**ReactiveClientRegistrationRepository** : Client registration information is stored by the associated Authorization server. The repository stores
a copy of the client registration information.

**ReactiveOAuth2AuthorizedClientService** : Responsible for the management of OAuth2AuthorizedClient. Which provide the purpose of associating
an Access Token credential to a client and Resource Owner (that originally granted the authorization).

The **ReactiveOAuth2AuthorizedClientManager** is build with this two components and with ReactiveOAuth2AuthorizedClientProvider.

The code is here :

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
	
@Bean WebClient with ServerOAuth2AuthorizedClientExchangeFilterFunction
=======================================================================

	@Bean
	public WebClient webClient(ReactiveOAuth2AuthorizedClientManager authorizedClientManager) {
		
		ServerOAuth2AuthorizedClientExchangeFilterFunction oauth =new ServerOAuth2AuthorizedClientExchangeFilterFunction(authorizedClientManager);
		oauth.setDefaultClientRegistrationId("oidcServices");
		
		return WebClient.
				builder().
				filter(oauth).
				build();
	}
	
When the **ReactiveOAuth2AuthorizedClientManager** is build, it is injected like parameter 0 in the **webClient** method.
The **ServerOAuth2AuthorizedClientExchangeFilterFunction** component is build and it necessary to precise the registration name (here is **oidcServices**).

Your WebClient is done.

@Bean SecurityWebFilterChain
----------------------------

	@Bean
	public SecurityWebFilterChain securityWebFilterChain(ServerHttpSecurity http) {
		
		return http.
				oauth2Client().
				and().
				build();
	}

When you using **spring-boot-starter-security**, your application is already secured. By default, a user and password (in the console) are created.
Here, we build a **SecurityWebFilterChain**.

DemoClientApplication class
---------------------------

	@EnableScheduling
	@EnableWebFluxSecurity
	@SpringBootApplication
	public class DemoClientApplication {...}
	
The **EnableScheduling** annotation provide the scheduled methods activation. In fact, the **logResourceServiceResponse** method is declared and
shoot a reactive http request with the **WebClient** that has been builded.

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
	
**http://localhost:8080/suppliers** this endpoint is other application [here](https://github.com/rudysaniez/demo-webflux-security).

You get :

	Retrieved using Client Credentials Grant Type (webclient-security) : [{"id":"62f30092-eaed-4eff-b122-df6b3ccf85f5","supplierId":1000,"description":"JACK"}
	Retrieved using Client Credentials Grant Type (webclient-security) : [{"id":"62f30092-eaed-4eff-b122-df6b3ccf85f5","supplierId":1000,"description":"JACK"}
	Retrieved using Client Credentials Grant Type (webclient-security) : [{"id":"62f30092-eaed-4eff-b122-df6b3ccf85f5","supplierId":1000,"description":"JACK"}
	Retrieved using Client Credentials Grant Type (webclient-security) : [{"id":"62f30092-eaed-4eff-b122-df6b3ccf85f5","supplierId":1000,"description":"JACK"}
	Retrieved using Client Credentials Grant Type (webclient-security) : [{"id":"62f30092-eaed-4eff-b122-df6b3ccf85f5","supplierId":1000,"description":"JACK"}
	
The **demo-webflux-security**, click [here](https://github.com/rudysaniez/demo-webflux-security), return a **Flux** with several suppliers.
You can see that the **suppliers** resource is protected by a **opaqueToken** checking.

I hope you are now happy! bye.
