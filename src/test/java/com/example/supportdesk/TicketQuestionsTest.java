package com.example.supportdesk;

import org.junit.jupiter.api.Test;
import org.springaicommunity.typesafe.question.Choice;
import org.springaicommunity.typesafe.question.Noul;
import org.springaicommunity.typesafe.question.Question;
import org.springaicommunity.typesafe.question.QuestionType;
import org.springaicommunity.typesafe.question.Score;

import static org.assertj.core.api.Assertions.assertThat;

class TicketQuestionsTest {

	@Test
	void triageMapContainsExpectedQuestions() {
		assertThat(TicketQuestions.TRIAGE).containsOnlyKeys("is_urgent", "department", "frustration");

		Question urgentQuestion = TicketQuestions.TRIAGE.get("is_urgent");
		assertThat(urgentQuestion).isInstanceOf(Noul.class);
		assertThat(urgentQuestion.type()).isEqualTo(QuestionType.NOUL);

		Question departmentQuestion = TicketQuestions.TRIAGE.get("department");
		assertThat(departmentQuestion).isInstanceOf(Choice.class);
		assertThat(departmentQuestion.type()).isEqualTo(QuestionType.CHOICE);
		Choice choice = (Choice) departmentQuestion;
		assertThat(choice.criteria().keySet()).containsExactlyInAnyOrder("billing", "technical", "sales", "feedback");

		Question frustrationQuestion = TicketQuestions.TRIAGE.get("frustration");
		assertThat(frustrationQuestion).isInstanceOf(Score.class);
		assertThat(frustrationQuestion.type()).isEqualTo(QuestionType.SCORE);
		Score score = (Score) frustrationQuestion;
		assertThat(score.criteria()).hasSize(3);
	}

}
