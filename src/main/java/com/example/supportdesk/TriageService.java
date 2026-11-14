package com.example.supportdesk;

import java.util.Map;

import org.springframework.ai.decision.DecisionModel;
import org.springframework.ai.decision.DecisionPrompt;
import org.springframework.ai.decision.DecisionResponse;
import org.springframework.ai.decision.answer.ChoiceAnswer;
import org.springframework.stereotype.Service;

@Service
public class TriageService {

	private final DecisionModel decisionModel;

	// Routing is cheap to undo; closing a ticket unread is not.
	private final ConfidenceGate gate = ConfidenceGate.builder()
		.floor(0.60d)
		.require("auto_close", 0.90d)
		.build();

	public TriageService(DecisionModel decisionModel) {
		this.decisionModel = decisionModel;
	}

	public Triage triage(String ticket) {
		long start = System.nanoTime();
		DecisionResponse r = this.decisionModel
			.call(DecisionPrompt.builder().text(ticket).questions(TicketQuestions.TRIAGE).build());
		long latencyMs = (System.nanoTime() - start) / 1_000_000;

		ChoiceAnswer department = r.getChoice("department");
		double urgency = r.getYesNo("is_urgent").getProbability();
		// Score values run from level 0 to the last level; normalise to 0..1.
		double frustration = r.getScore("frustration").getValue() / (TicketQuestions.FRUSTRATION_LEVELS - 1);
		Double reported = department.getConfidence();
		double confidence = (reported != null) ? reported : orZero(department.getProbability());
		Map<String, Double> probabilities = (department.getProbabilities() != null) ? department.getProbabilities()
				: Map.of();

		// Composed in code: two independent answers make one policy decision.
		boolean untroubled = urgency < 0.2d && frustration < 0.5d;
		String action = untroubled ? "auto_close" : "route_to_" + department.getValue();

		return new Triage(action, department.getValue(), confidence, probabilities, urgency, frustration,
				this.gate.decide(action, confidence), r.getMetadata().getModel(), latencyMs);
	}

	private static double orZero(Double value) {
		return (value != null) ? value : 0d;
	}

	public record Triage(String action, String department, double confidence,
			Map<String, Double> departmentProbabilities, double urgency, double frustration,
			ConfidenceGate.Decision decision, String model, long latencyMs) {
	}

}
