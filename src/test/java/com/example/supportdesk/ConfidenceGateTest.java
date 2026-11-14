package com.example.supportdesk;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.assertj.core.api.Assertions.assertThat;

class ConfidenceGateTest {

	private final ConfidenceGate gate = ConfidenceGate.builder().floor(0.60d).require("auto_close", 0.90d).build();

	@ParameterizedTest
	@CsvSource({ "route_to_billing, 0.59, ESCALATE", "route_to_billing, 0.60, EXECUTE", "route_to_billing, 0.99, EXECUTE",
			"auto_close, 0.59, ESCALATE", "auto_close, 0.60, CONFIRM", "auto_close, 0.89, CONFIRM",
			"auto_close, 0.90, EXECUTE" })
	void decidesFromFloorAndPerActionRequirement(String action, double confidence,
			ConfidenceGate.Decision expected) {
		assertThat(this.gate.decide(action, confidence)).isEqualTo(expected);
	}

}
