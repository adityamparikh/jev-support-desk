package com.example.supportdesk;

import java.util.Map;

import org.springaicommunity.typesafe.TypeSafeClient;
import org.springaicommunity.typesafe.judge.JevConfidenceGate;
import org.springaicommunity.typesafe.response.ChoiceAnswer;
import org.springaicommunity.typesafe.response.SystemOneResponse;

import org.springframework.stereotype.Service;

@Service
public class TriageService {

	private final TypeSafeClient typeSafe;

	// Routing is cheap to undo; closing a ticket unread is not.
	private final JevConfidenceGate gate = JevConfidenceGate.builder()
		.floor(0.60d)
		.require("auto_close", 0.90d)
		.build();

	public TriageService(TypeSafeClient typeSafe) {
		this.typeSafe = typeSafe;
	}

	public Triage triage(String ticket) {
		long start = System.nanoTime();
		SystemOneResponse r = this.typeSafe.systemOne(ticket, TicketQuestions.TRIAGE);
		long latencyMs = (System.nanoTime() - start) / 1_000_000;

		ChoiceAnswer department = r.choice("department");
		double urgency = r.noulValue("is_urgent");
		double frustration = r.scoreValue("frustration");

		// Composed in code: two independent answers make one policy decision.
		boolean untroubled = urgency < 0.2d && frustration < 0.5d;
		String action = untroubled ? "auto_close" : "route_to_" + department.value();

		return new Triage(action, department.value(), department.confidence(), department.probabilities(),
				urgency, frustration, this.gate.decide(action, department.confidence()), r.model(), latencyMs);
	}

	public record Triage(String action, String department, double confidence,
			Map<String, Double> departmentProbabilities, double urgency, double frustration,
			JevConfidenceGate.Decision decision, String model, long latencyMs) {
	}

}
