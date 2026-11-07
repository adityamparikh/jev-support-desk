package com.example.supportdesk;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.decision.DecisionModel;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
class SupportReplyConfig {

	@Bean
	ReplyJudge supportReplyJudge(DecisionModel decisionModel) {
		return new ReplyJudge(decisionModel);
	}

	@Bean
	ChatClient supportChatClient(ChatClient.Builder builder) {
		return builder.defaultSystem("You are a support engineer. Draft a short first reply to the ticket.").build();
	}

}
