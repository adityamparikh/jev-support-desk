package com.example.supportdesk;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springaicommunity.typesafe.TypeSafeClient;
import org.springaicommunity.typesafe.advisor.JevGuardrailAdvisor;
import org.springaicommunity.typesafe.advisor.JevSelfRefineAdvisor;
import org.springaicommunity.typesafe.judge.JevCriterion;
import org.springaicommunity.typesafe.judge.JevCriterion.CodeCriterion;
import org.springaicommunity.typesafe.judge.JevCriterion.QuestionCriterion;
import org.springaicommunity.typesafe.judge.JevJudge;
import org.springaicommunity.typesafe.judge.JevJudgeInput;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.api.Advisor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class SupportReplyConfigTest {

	private SupportReplyConfig config;

	private TypeSafeClient typeSafe;

	@BeforeEach
	void setUp() {
		this.config = new SupportReplyConfig();
		this.typeSafe = Mockito.mock(TypeSafeClient.class);
	}

	@Test
	void supportReplyJudgeConfiguresExpectedCriteria() {
		JevJudge judge = this.config.supportReplyJudge(this.typeSafe);
		List<JevCriterion> criteria = judge.criteria();

		assertThat(criteria).hasSize(4);

		QuestionCriterion addressesIssue = findQuestionCriterion(criteria, "addresses_issue");
		assertThat(addressesIssue.minimum()).isEqualTo(0.7d);

		QuestionCriterion noUnapprovedPromise = findQuestionCriterion(criteria, "no_unapproved_promise");
		assertThat(noUnapprovedPromise.minimum()).isEqualTo(0.8d);

		QuestionCriterion tone = findQuestionCriterion(criteria, "tone");
		assertThat(tone.minimum()).isEqualTo(1.5d);

		CodeCriterion notEmpty = (CodeCriterion) criteria.stream()
			.filter(c -> c instanceof CodeCriterion && "not_empty".equals(c.name()))
			.findFirst()
			.orElseThrow(() -> new AssertionError("not_empty criterion not found"));

		assertThat(notEmpty.defect()).isEqualTo("the reply is empty");

		assertThat(notEmpty.check().test(JevJudgeInput.builder().answer("Valid answer").build())).isTrue();
		assertThat(notEmpty.check().test(JevJudgeInput.builder().answer("").build())).isFalse();
		assertThat(notEmpty.check().test(JevJudgeInput.builder().answer("   ").build())).isFalse();
		assertThat(notEmpty.check().test(JevJudgeInput.builder().build())).isFalse();
	}

	@Test
	void supportChatClientConfiguresBuilderWithSystemPromptAndAdvisors() {
		ChatClient.Builder builder = Mockito.mock(ChatClient.Builder.class);
		ChatClient chatClient = Mockito.mock(ChatClient.class);
		JevJudge judge = Mockito.mock(JevJudge.class);

		when(builder.defaultSystem(anyString())).thenReturn(builder);
		when(builder.defaultAdvisors(any(Advisor[].class))).thenReturn(builder);
		when(builder.build()).thenReturn(chatClient);

		ChatClient result = this.config.supportChatClient(builder, this.typeSafe, judge);

		assertThat(result).isSameAs(chatClient);
		verify(builder).defaultSystem("You are a support engineer. Draft a short first reply to the ticket.");

		ArgumentCaptor<Advisor[]> advisorsCaptor = ArgumentCaptor.forClass(Advisor[].class);
		verify(builder).defaultAdvisors(advisorsCaptor.capture());

		Advisor[] advisors = advisorsCaptor.getValue();
		assertThat(advisors).hasSize(2);
		assertThat(advisors[0]).isInstanceOf(JevSelfRefineAdvisor.class);
		assertThat(advisors[1]).isInstanceOf(JevGuardrailAdvisor.class);
		verify(builder).build();
	}

	private QuestionCriterion findQuestionCriterion(List<JevCriterion> criteria, String name) {
		return (QuestionCriterion) criteria.stream()
			.filter(c -> c instanceof QuestionCriterion && name.equals(c.name()))
			.findFirst()
			.orElseThrow(() -> new AssertionError("Criterion " + name + " not found"));
	}

}
