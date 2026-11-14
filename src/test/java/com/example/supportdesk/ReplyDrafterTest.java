package com.example.supportdesk;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Answers;
import org.mockito.Mockito;

import org.springframework.ai.chat.client.ChatClient;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ReplyDrafterTest {

	private static final String TICKET = "My payouts have been failing for 3 days.";

	private static final ReplyJudge.Verdict PASS = new ReplyJudge.Verdict(List.of());

	private static final ReplyJudge.Verdict FAIL = new ReplyJudge.Verdict(List.of("tone: too cold"));

	private ChatClient chatClient;

	private ReplyJudge judge;

	private ReplyDrafter drafter;

	@BeforeEach
	void setUp() {
		this.chatClient = Mockito.mock(ChatClient.class, Answers.RETURNS_DEEP_STUBS);
		this.judge = Mockito.mock(ReplyJudge.class);
		this.drafter = new ReplyDrafter(this.chatClient, this.judge);
		when(this.judge.screenInput(TICKET)).thenReturn(PASS);
	}

	@Test
	void returnsFirstDraftWhenJudgePasses() {
		when(this.chatClient.prompt().user(TICKET).call().content()).thenReturn("Draft 1");
		when(this.judge.judge(TICKET, "Draft 1")).thenReturn(PASS);

		assertThat(this.drafter.draft(TICKET)).isEqualTo("Draft 1");
	}

	@Test
	void redraftsWithDefectsFedBackUntilJudgePasses() {
		when(this.chatClient.prompt().user(TICKET).call().content()).thenReturn("Draft 1");
		when(this.chatClient.prompt().user(contains("tone: too cold")).call().content()).thenReturn("Draft 2");
		when(this.judge.judge(TICKET, "Draft 1")).thenReturn(FAIL);
		when(this.judge.judge(TICKET, "Draft 2")).thenReturn(PASS);

		assertThat(this.drafter.draft(TICKET)).isEqualTo("Draft 2");
	}

	@Test
	void throwsAfterMaxAttemptsInsteadOfReturningBestEffort() {
		when(this.chatClient.prompt().user(Mockito.anyString()).call().content()).thenReturn("Draft");
		when(this.judge.judge(eq(TICKET), Mockito.anyString())).thenReturn(FAIL);

		assertThatThrownBy(() -> this.drafter.draft(TICKET)).isInstanceOf(ReplyRejectedException.class)
			.hasMessage("tone: too cold");
		verify(this.judge, times(ReplyDrafter.MAX_ATTEMPTS)).judge(eq(TICKET), Mockito.anyString());
	}

	@Test
	void refusedTicketIsNeverDrafted() {
		when(this.judge.screenInput(TICKET)).thenReturn(new ReplyJudge.Verdict(List.of("injection")));

		assertThatThrownBy(() -> this.drafter.draft(TICKET)).isInstanceOf(ReplyRejectedException.class);
		verify(this.judge, never()).judge(Mockito.anyString(), Mockito.anyString());
	}

}
