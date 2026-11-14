package com.example.supportdesk;

import org.junit.jupiter.api.Test;

import org.springframework.ai.decision.question.ChoiceQuestion;
import org.springframework.ai.decision.question.DecisionQuestion;
import org.springframework.ai.decision.question.ScoreQuestion;
import org.springframework.ai.decision.question.YesNoQuestion;

import static org.assertj.core.api.Assertions.assertThat;

class TicketQuestionsTest {

	@Test
	void triageMapContainsExpectedQuestions() {
		assertThat(TicketQuestions.TRIAGE).containsOnlyKeys("is_urgent", "department", "frustration");

		DecisionQuestion urgent = TicketQuestions.TRIAGE.get("is_urgent");
		assertThat(urgent).isInstanceOf(YesNoQuestion.class);

		DecisionQuestion department = TicketQuestions.TRIAGE.get("department");
		assertThat(department).isInstanceOf(ChoiceQuestion.class);
		assertThat(((ChoiceQuestion) department).getOptionNames())
			.containsExactlyInAnyOrder("billing", "technical", "sales", "feedback");

		DecisionQuestion frustration = TicketQuestions.TRIAGE.get("frustration");
		assertThat(frustration).isInstanceOf(ScoreQuestion.class);
		assertThat(((ScoreQuestion) frustration).getLevels()).hasSize(TicketQuestions.FRUSTRATION_LEVELS);
	}

}
