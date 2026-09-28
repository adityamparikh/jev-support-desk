package com.example.supportdesk;

import org.springaicommunity.typesafe.TypeSafeClient;
import org.springaicommunity.typesafe.advisor.JevGuardrailAdvisor;
import org.springaicommunity.typesafe.advisor.JevSelfRefineAdvisor;
import org.springaicommunity.typesafe.judge.JevJudge;
import org.springaicommunity.typesafe.question.Noul;
import org.springaicommunity.typesafe.question.Score;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;

@Configuration
class SupportReplyConfig {

	@Bean
	JevJudge supportReplyJudge(TypeSafeClient typeSafe) {
		return JevJudge.builder(typeSafe)
			.noul("addresses_issue", Noul.builder()
				.instructions("Does `assistant_answer` respond to the specific problem in `user_question`?")
				.whenTrue("Speaks directly to the problem the customer described")
				.whenFalse("A generic reply that ignores the customer's actual problem")
				.build(), 0.7d)
			.noul("no_unapproved_promise", Noul.builder()
				.instructions("Does `assistant_answer` avoid promising refunds, credits or fix dates?")
				.whenTrue("Makes no commitment about money or timing")
				.whenFalse("Promises a refund, credit or fix date that support has not approved")
				.build(), 0.8d)
			.score("tone", Score.builder()
				.instructions("How appropriate is the tone of `assistant_answer` for an upset customer?")
				.level("Dismissive or defensive")
				.level("Neutral but cold")
				.level("Empathetic and professional")
				.build(), 1.5d)
			// Deterministic checks stay in code: exact, free, never sent to Jev.
			.check("not_empty", in -> StringUtils.hasText(in.answer()), "the reply is empty")
			.build();
	}

	@Bean
	ChatClient supportChatClient(ChatClient.Builder builder, TypeSafeClient typeSafe, JevJudge supportReplyJudge) {
		return builder.defaultSystem("You are a support engineer. Draft a short first reply to the ticket.")
			.defaultAdvisors(
					JevSelfRefineAdvisor.builder()
						.judge(supportReplyJudge)
						.maxRepeatAttempts(3)
						.failOnExhaustedAttempts(true) // throw instead of returning best effort
						.build(),
					JevGuardrailAdvisor.builder(typeSafe).build()) // default input + output batteries
			.build();
	}

}
