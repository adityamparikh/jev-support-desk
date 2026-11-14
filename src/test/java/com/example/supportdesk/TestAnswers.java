package com.example.supportdesk;

import java.util.Map;

import org.springframework.ai.decision.DecisionPrompt;
import org.springframework.ai.decision.DecisionResponse;
import org.springframework.ai.decision.DecisionResponseMetadata;
import org.springframework.ai.decision.answer.ChoiceAnswer;
import org.springframework.ai.decision.answer.ScoreAnswer;
import org.springframework.ai.decision.answer.YesNoAnswer;
import org.springframework.ai.decision.question.DecisionQuestion;

/** Builds canned {@link DecisionResponse}s for tests. */
final class TestAnswers {

	static DecisionResponse.Builder response(String model, Map<String, DecisionQuestion> questions) {
		return DecisionResponse.builder(DecisionPrompt.builder().text("ticket").questions(questions).build())
			.metadata(DecisionResponseMetadata.builder().id("req-1").model(model).build());
	}

	static YesNoAnswer yesNo(double probability) {
		return YesNoAnswer.builder().probability(probability).build();
	}

	static ScoreAnswer score(double level) {
		return ScoreAnswer.builder().value(level).build();
	}

	static ChoiceAnswer choice(String value, double confidence) {
		return ChoiceAnswer.builder()
			.value(value)
			.probabilities(Map.of(value, confidence))
			.confidence(confidence)
			.build();
	}

	private TestAnswers() {
	}

}
