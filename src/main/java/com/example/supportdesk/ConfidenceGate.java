package com.example.supportdesk;

import java.util.HashMap;
import java.util.Map;

/**
 * Turns a confidence into a decision. Below the floor the answer is escalated; at or
 * above the level required for an action it is executed; in between it needs a
 * confirmation. Actions without their own requirement use the floor.
 */
public final class ConfidenceGate {

	public enum Decision {

		EXECUTE, CONFIRM, ESCALATE

	}

	private final double floor;

	private final Map<String, Double> required;

	private ConfidenceGate(double floor, Map<String, Double> required) {
		this.floor = floor;
		this.required = Map.copyOf(required);
	}

	public static Builder builder() {
		return new Builder();
	}

	public Decision decide(String action, double confidence) {
		if (confidence < this.floor) {
			return Decision.ESCALATE;
		}
		return (confidence >= this.required.getOrDefault(action, this.floor)) ? Decision.EXECUTE : Decision.CONFIRM;
	}

	public static final class Builder {

		private double floor;

		private final Map<String, Double> required = new HashMap<>();

		public Builder floor(double floor) {
			this.floor = floor;
			return this;
		}

		public Builder require(String action, double confidence) {
			this.required.put(action, confidence);
			return this;
		}

		public ConfidenceGate build() {
			return new ConfidenceGate(this.floor, this.required);
		}

	}

}
