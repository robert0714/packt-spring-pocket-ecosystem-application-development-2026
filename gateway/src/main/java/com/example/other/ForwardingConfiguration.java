package com.example.other;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientManager;
import org.springframework.security.oauth2.client.web.client.OAuth2ClientHttpRequestInterceptor;
import org.springframework.web.client.RestClient;
import org.springframework.web.servlet.function.RouterFunction;
import org.springframework.web.servlet.function.ServerResponse;

import java.util.Collection;
import java.util.Map;
import java.util.Objects;

import static org.springframework.cloud.gateway.server.mvc.handler.GatewayRouterFunctions.route;
import static org.springframework.security.oauth2.client.web.client.RequestAttributeClientRegistrationIdResolver.clientRegistrationId;

@Configuration
class ForwardingConfiguration {

	@Bean
	RouterFunction<ServerResponse> forwardingRoute(RestClient.Builder builder,
			OAuth2AuthorizedClientManager authorizedClientManager) {
		var requestInterceptor = new OAuth2ClientHttpRequestInterceptor(authorizedClientManager);
		var rc = builder.requestInterceptor(requestInterceptor).build();

		return route() //
			.GET("/gateway-dogs", request -> {
				var bodyType = new ParameterizedTypeReference<Collection<Map<String, Object>>>() {
				};
				var collectionOfDogMaps = rc //
					.get() //
					.uri("http://localhost:8080/http/dogs")//
					.attributes(clientRegistrationId("spring"))//
					.retrieve()//
					.body(bodyType);
				System.out.println("got " + collectionOfDogMaps);
				return ServerResponse.ok() //
					.body(Objects.requireNonNull(collectionOfDogMaps));
			}) //
			.build();
	}

}
