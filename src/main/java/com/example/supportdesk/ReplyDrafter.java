package com.example.supportdesk;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;

/**
 * Drafts a reply with the chat model and re-drafts, feeding the judge's defects back,
 * until the {@link ReplyJudge} passes it. After {@value #MAX_ATTEMPTS} failed attempts it
 * throws instead of returning the best effort.
 */
@Service
class ReplyDrafter {

	static final int MAX_ATTEMPTS = 3;

	private final ChatClient supportChatClient;

	private final ReplyJudge judge;

	ReplyDrafter(ChatClient supportChatClient, ReplyJudge judge) {
		this.supportChatClient = supportChatClient;
		this.judge = judge;
	}

	String draft(String ticket) {
		ReplyJudge.Verdict input = this.judge.screenInput(ticket);
		if (!input.passed()) {
			throw new ReplyRejectedException(input);
		}
		String prompt = ticket;
		ReplyJudge.Verdict verdict = null;
		for (int attempt = 1; attempt <= MAX_ATTEMPTS; attempt++) {
			String reply = this.supportChatClient.prompt().user(prompt).call().content();
			verdict = this.judge.judge(ticket, reply);
			if (verdict.passed()) {
				return reply;
			}
			prompt = ticket + "\n\nYour previous draft was rejected:\n" + reply + "\n\nProblems: "
					+ verdict.summary() + "\nWrite a new reply that fixes them.";
		}
		throw new ReplyRejectedException(verdict);
	}

}
