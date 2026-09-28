package com.example.supportdesk;

import java.util.Map;

import org.junit.jupiter.api.Test;
import org.mockito.Answers;
import org.mockito.Mockito;
import org.springaicommunity.typesafe.advisor.JevSelfRefineFailedException;
import org.springaicommunity.typesafe.judge.JevConfidenceGate.Decision;
import org.springaicommunity.typesafe.judge.JevVerdict;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(TicketController.class)
class TicketControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private TriageService triageService;

	@MockitoBean(name = "supportChatClient", answers = Answers.RETURNS_DEEP_STUBS)
	private ChatClient supportChatClient;

	@Test
	void submitTicketWithExecuteDecisionReturnsDraft() throws Exception {
		String ticket = "My payouts have been failing for 3 days.";
		TriageService.Triage triage = new TriageService.Triage(
				"route_to_billing",
				"billing",
				0.92d,
				Map.of("billing", 0.92d),
				0.85d,
				0.70d,
				Decision.EXECUTE,
				"jev-model",
				42L
		);

		when(this.triageService.triage(ticket)).thenReturn(triage);
		when(this.supportChatClient.prompt().user(ticket).call().content()).thenReturn("Here is the draft reply.");

		this.mockMvc.perform(post("/tickets")
				.contentType(MediaType.TEXT_PLAIN)
				.content(ticket))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.triage.action").value("route_to_billing"))
			.andExpect(jsonPath("$.triage.department").value("billing"))
			.andExpect(jsonPath("$.triage.confidence").value(0.92d))
			.andExpect(jsonPath("$.triage.urgency").value(0.85d))
			.andExpect(jsonPath("$.triage.frustration").value(0.70d))
			.andExpect(jsonPath("$.triage.decision").value("EXECUTE"))
			.andExpect(jsonPath("$.triage.model").value("jev-model"))
			.andExpect(jsonPath("$.triage.latencyMs").value(42))
			.andExpect(jsonPath("$.draft").value("Here is the draft reply."));

		verify(this.triageService).triage(ticket);
		verify(this.supportChatClient.prompt().user(ticket).call()).content();
	}

	@Test
	void submitTicketWithConfirmDecisionReturnsDraft() throws Exception {
		String ticket = "I think there is a small typo.";
		TriageService.Triage triage = new TriageService.Triage(
				"auto_close",
				"feedback",
				0.70d,
				Map.of("feedback", 0.70d),
				0.05d,
				0.10d,
				Decision.CONFIRM,
				"jev-model",
				30L
		);

		when(this.triageService.triage(ticket)).thenReturn(triage);
		when(this.supportChatClient.prompt().user(ticket).call().content()).thenReturn("Thanks for noticing!");

		this.mockMvc.perform(post("/tickets")
				.contentType(MediaType.TEXT_PLAIN)
				.content(ticket))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.triage.action").value("auto_close"))
			.andExpect(jsonPath("$.triage.decision").value("CONFIRM"))
			.andExpect(jsonPath("$.draft").value("Thanks for noticing!"));

		verify(this.triageService).triage(ticket);
	}

	@Test
	void submitTicketWithEscalateDecisionReturnsNullDraftWithoutCallingChatClient() throws Exception {
		String ticket = "Something is wrong, but I don't know what.";
		TriageService.Triage triage = new TriageService.Triage(
				"route_to_technical",
				"technical",
				0.45d,
				Map.of("technical", 0.45d),
				0.80d,
				0.60d,
				Decision.ESCALATE,
				"jev-model",
				25L
		);

		when(this.triageService.triage(ticket)).thenReturn(triage);

		this.mockMvc.perform(post("/tickets")
				.contentType(MediaType.TEXT_PLAIN)
				.content(ticket))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.triage.action").value("route_to_technical"))
			.andExpect(jsonPath("$.triage.decision").value("ESCALATE"))
			.andExpect(jsonPath("$.draft").doesNotExist());

		verify(this.triageService).triage(ticket);
		verifyNoInteractions(this.supportChatClient);
	}

	@Test
	void judgeGaveUpExceptionReturnsAcceptedWithHumanQueueMessage() throws Exception {
		String ticket = "I demand a refund immediately!";
		TriageService.Triage triage = new TriageService.Triage(
				"route_to_billing",
				"billing",
				0.88d,
				Map.of("billing", 0.88d),
				0.90d,
				0.95d,
				Decision.EXECUTE,
				"jev-model",
				50L
		);

		JevVerdict verdict = Mockito.mock(JevVerdict.class);
		when(verdict.summary()).thenReturn("addresses_issue failed");
		JevSelfRefineFailedException exception = new JevSelfRefineFailedException(3, verdict);

		when(this.triageService.triage(ticket)).thenReturn(triage);
		when(this.supportChatClient.prompt().user(ticket).call().content()).thenThrow(exception);

		this.mockMvc.perform(post("/tickets")
				.contentType(MediaType.TEXT_PLAIN)
				.content(ticket))
			.andExpect(status().isAccepted())
			.andExpect(content().string("Queued for a human: addresses_issue failed"));
	}

	@Test
	void submitWithUnsupportedMediaTypeReturnsClientError() throws Exception {
		this.mockMvc.perform(post("/tickets")
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"ticket\":\"test\"}"))
			.andExpect(status().isUnsupportedMediaType());
	}

}
