package com.example.supportdesk;

import org.springframework.ai.decision.DecisionModel;
import org.springframework.ai.systemone.SystemOneDecisionModel;
import org.springframework.ai.systemone.api.SystemOneClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Wires Jev (TypeSafe's System One model) as a Spring AI {@link DecisionModel}. This app
 * depends on {@code spring-ai-systemone} only, not on its starter, so there is no
 * auto-configuration and both beans are declared here.
 */
@Configuration
class JevConfig {

	@Bean
	SystemOneClient systemOneClient(@Value("${spring.ai.typesafe.api-key}") String apiKey) {
		return SystemOneClient.builder().apiKey(apiKey).build();
	}

	@Bean
	DecisionModel decisionModel(SystemOneClient systemOneClient) {
		return SystemOneDecisionModel.builder(systemOneClient).build();
	}

}
