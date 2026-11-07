package com.example.supportdesk;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

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

	@MockitoBean
	private ReplyDrafter drafter;

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
				ConfidenceGate.Decision.EXECUTE,
				"jev-model",
				42L
		);

		when(this.triageService.triage(ticket)).thenReturn(triage);
		when(this.drafter.draft(ticket)).thenReturn("Here is the draft reply.");

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
		verify(this.drafter).draft(ticket);
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
				ConfidenceGate.Decision.CONFIRM,
				"jev-model",
				30L
		);

		when(this.triageService.triage(ticket)).thenReturn(triage);
		when(this.drafter.draft(ticket)).thenReturn("Thanks for noticing!");

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
				ConfidenceGate.Decision.ESCALATE,
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
		verifyNoInteractions(this.drafter);
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
				ConfidenceGate.Decision.EXECUTE,
				"jev-model",
				50L
		);

		ReplyRejectedException exception = new ReplyRejectedException(
				new ReplyJudge.Verdict(List.of("addresses_issue failed")));

		when(this.triageService.triage(ticket)).thenReturn(triage);
		when(this.drafter.draft(ticket)).thenThrow(exception);

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
