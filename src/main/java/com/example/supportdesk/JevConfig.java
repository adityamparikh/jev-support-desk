package com.example.supportdesk;

import org.springaicommunity.typesafe.TypeSafeClient;

import org.springframework.ai.decision.DecisionModel;
import org.springframework.ai.systemone.SystemOneDecisionModel;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Wires Jev (TypeSafe's System One model) as a Spring AI {@link DecisionModel}. The
 * System One module has no auto-configuration, so both beans are declared here.
 */
@Configuration
class JevConfig {

	@Bean
	TypeSafeClient typeSafeClient(@Value("${spring.ai.typesafe.api-key}") String apiKey) {
		return TypeSafeClient.builder().apiKey(apiKey).build();
	}

	@Bean
	DecisionModel decisionModel(TypeSafeClient typeSafeClient) {
		return SystemOneDecisionModel.builder(typeSafeClient).build();
	}

}
