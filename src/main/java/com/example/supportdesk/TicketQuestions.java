package com.example.supportdesk;

import java.util.Map;

import org.springaicommunity.typesafe.question.Choice;
import org.springaicommunity.typesafe.question.Noul;
import org.springaicommunity.typesafe.question.Question;
import org.springaicommunity.typesafe.question.Score;

/**
 * The three typed triage questions, asked together in one Jev call. Adapted from the
 * TicketTriageDemo in spring-ai-community/spring-ai-typesafe.
 */
final class TicketQuestions {

	static final Map<String, Question> TRIAGE = Map.of(
			"is_urgent", Noul.builder()
				.instructions("Does this message convey urgency or time-sensitivity?")
				.whenTrue("Explicitly time-sensitive, or describes an ongoing loss")
				.whenFalse("No urgency expressed")
				.build(),
			"department", Choice.builder()
				.instructions("Which team should handle this?")
				.option("billing", "Payments, invoicing, refunds, payouts")
				.option("technical", "Bugs, outages, integrations that do not work")
				.option("sales", "Pricing, upgrades, new accounts")
				.option("feedback", "Praise, suggestions, anything needing no action")
				.build(),
			"frustration", Score.of("How frustrated does the customer appear?",
					"Calm, just stating facts", "Frustrated but civil", "Very angry, strong language"));

	private TicketQuestions() {
	}

}
