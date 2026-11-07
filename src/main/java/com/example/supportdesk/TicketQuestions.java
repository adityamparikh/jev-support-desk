package com.example.supportdesk;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.ai.decision.question.ChoiceQuestion;
import org.springframework.ai.decision.question.DecisionQuestion;
import org.springframework.ai.decision.question.ScoreQuestion;
import org.springframework.ai.decision.question.YesNoQuestion;

/**
 * The three typed triage questions, asked together in one Jev call. Adapted from the
 * TicketTriageDemo in spring-ai-community/spring-ai-typesafe.
 */
final class TicketQuestions {

	/** Number of levels in the {@code frustration} scale. */
	static final int FRUSTRATION_LEVELS = 3;

	static final Map<String, DecisionQuestion> TRIAGE;

	static {
		Map<String, DecisionQuestion> questions = new LinkedHashMap<>();
		questions.put("is_urgent", YesNoQuestion.builder()
			.text("Does this message convey urgency or time-sensitivity?")
			.whenYes("Explicitly time-sensitive, or describes an ongoing loss")
			.whenNo("No urgency expressed")
			.build());
		questions.put("department", ChoiceQuestion.builder()
			.text("Which team should handle this?")
			.option("billing", "Payments, invoicing, refunds, payouts")
			.option("technical", "Bugs, outages, integrations that do not work")
			.option("sales", "Pricing, upgrades, new accounts")
			.option("feedback", "Praise, suggestions, anything needing no action")
			.build());
		questions.put("frustration", ScoreQuestion.builder()
			.text("How frustrated does the customer appear?")
			.level("Calm, just stating facts")
			.level("Frustrated but civil")
			.level("Very angry, strong language")
			.build());
		TRIAGE = Collections.unmodifiableMap(questions);
	}

	private TicketQuestions() {
	}

}
