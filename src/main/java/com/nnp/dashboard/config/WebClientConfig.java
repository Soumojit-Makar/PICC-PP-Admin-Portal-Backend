package com.nnp.dashboard.config;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nnp.dashboard.client.KeycloakClient;
import com.nnp.dashboard.exception.DashboardConfigException;
import com.nnp.dashboard.exception.DashboardConfigExceptionMessage;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatusCode;
import org.springframework.web.reactive.function.client.ExchangeFilterFunction;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.support.WebClientAdapter;
import org.springframework.web.service.invoker.HttpServiceProxyFactory;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

/**
 * Configures the reactive {@link WebClient} used to talk to Keycloak and
 * builds the {@link KeycloakClient} proxy from it.
 *
 * Two exchange filters are registered on the client:
 *   - {@code logRequest}     : debug-log the outgoing method / URL / headers
 *   - {@code logResponseStatus}: debug-log the response and, when the response
 *     is an error, parse the JSON body into a {@link DashboardConfigException}
 *     carrying the upstream error code + message.
 */
@Configuration
@Slf4j
public class WebClientConfig {

    @Value("${keycloak.service.baseurl:http://localhost:8080}")
    private String baseUrl;

    private ExchangeFilterFunction logResponseStatus() {
        return ExchangeFilterFunction.ofResponseProcessor(clientResponse -> {
//            log.debug("Response Status --> {}", clientResponse.statusCode());
//            clientResponse.headers()
//                    .asHttpHeaders()
//                    .forEach((s, strings) -> strings
//                            .forEach(s1 -> log.debug("{}={}", s, s1)));
            //---------------------------------------------------------------------------------------
            if (clientResponse.statusCode().isError()) {
//                log.info("$$ error occurred");
                return clientResponse.bodyToMono(String.class)
                        .flatMap(s -> {
//                            log.info("Error String is {}", s);
                            Map<String, String> map;
                            ObjectMapper mapper = new ObjectMapper();
                            mapper.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
                            TypeReference<HashMap<String, String>> typeRef =
                                    new TypeReference<HashMap<String, String>>() {
                                    };
                            try {
                                map = mapper.readValue(s, typeRef);
                            } catch (JsonProcessingException e) {
//                                log.info("JsonProcessingException --> {}", e.getMessage());
                                throw new RuntimeException(e);
                            }
//                            return Mono.error(new DashboardConfigException("code = "
//                                    + map.get("code")
//                                    + " message = "
//                                    + map.get("message")));
//                            return Mono.error(new DashboardConfigException(s));
                            return Mono.error(new DashboardConfigException(new DashboardConfigExceptionMessage(map.get("code"), map.get("message"))));
                        });
            } else {
                return Mono.just(clientResponse);
            }
            //---------------------------------------------------------------------------------------
            //return Mono.just(clientResponse);
            //---------------------------------------------------------------------------------------
        });
    }

    private ExchangeFilterFunction logRequest() {
        //            log.debug("Request: {} {}", clientRequest.method(), clientRequest.url());
        //            clientRequest.headers()
        //                    .forEach((name, values) -> values
        //                            .forEach(value -> log.debug("{}={}", name, value)));
        return ExchangeFilterFunction.ofRequestProcessor(Mono::just);
    }


    @Bean
    public WebClient keyclockWebClient() {
        return WebClient.builder()
                .baseUrl(baseUrl)
                .filter(logRequest())
                .filter(logResponseStatus())
                .build();
    }
    @Bean
    public KeycloakClient keycloakClient() {
		/*
		 * HttpServiceProxyFactory httpServiceProxyFactory = HttpServiceProxyFactory
		 * .builder(WebClientAdapter.forClient(keyclockWebClient()))
		 * .blockTimeout(Duration.ofSeconds(10)) .build();
		 */
    	HttpServiceProxyFactory httpServiceProxyFactory = HttpServiceProxyFactory.builderFor(WebClientAdapter.create(keyclockWebClient())).build();
    	
        return httpServiceProxyFactory.createClient(KeycloakClient.class);
    }
}
