package com.example.supportdesk;

import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.Mockito;
import org.springaicommunity.typesafe.TypeSafeClient;
import org.springaicommunity.typesafe.judge.JevConfidenceGate.Decision;
import org.springaicommunity.typesafe.response.Answer;
import org.springaicommunity.typesafe.response.ChoiceAnswer;
import org.springaicommunity.typesafe.response.NoulAnswer;
import org.springaicommunity.typesafe.response.ScoreAnswer;
import org.springaicommunity.typesafe.response.SystemOneResponse;
import org.springaicommunity.typesafe.response.Usage;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class TriageServiceTest {

	private TypeSafeClient typeSafe;

	private TriageService triageService;

	@BeforeEach
	void setUp() {
		this.typeSafe = Mockito.mock(TypeSafeClient.class);
		this.triageService = new TriageService(this.typeSafe);
	}

	@Test
	void untroubledTicketWithHighConfidenceAutoClosesAndExecutes() {
		String ticket = "Great product, thank you!";
		SystemOneResponse response = createResponse("feedback", 0.95d, 0.05d, 0.10d);
		when(this.typeSafe.systemOne(eq(ticket), eq(TicketQuestions.TRIAGE))).thenReturn(response);

		TriageService.Triage triage = this.triageService.triage(ticket);

		assertThat(triage.action()).isEqualTo("auto_close");
		assertThat(triage.department()).isEqualTo("feedback");
		assertThat(triage.confidence()).isEqualTo(0.95d);
		assertThat(triage.urgency()).isEqualTo(0.05d);
		assertThat(triage.frustration()).isEqualTo(0.10d);
		assertThat(triage.decision()).isEqualTo(Decision.EXECUTE);
		assertThat(triage.model()).isEqualTo("test-model");
		assertThat(triage.latencyMs()).isGreaterThanOrEqualTo(0);
		assertThat(triage.departmentProbabilities()).containsEntry("feedback", 0.95d);

		verify(this.typeSafe).systemOne(ticket, TicketQuestions.TRIAGE);
	}

	@Test
	void untroubledTicketWithModerateConfidenceConfirms() {
		String ticket = "I like this feature.";
		SystemOneResponse response = createResponse("feedback", 0.75d, 0.10d, 0.20d);
		when(this.typeSafe.systemOne(eq(ticket), eq(TicketQuestions.TRIAGE))).thenReturn(response);

		TriageService.Triage triage = this.triageService.triage(ticket);

		assertThat(triage.action()).isEqualTo("auto_close");
		assertThat(triage.decision()).isEqualTo(Decision.CONFIRM);
	}

	@Test
	void untroubledTicketWithLowConfidenceEscalates() {
		String ticket = "Maybe I will use this.";
		SystemOneResponse response = createResponse("feedback", 0.50d, 0.15d, 0.25d);
		when(this.typeSafe.systemOne(eq(ticket), eq(TicketQuestions.TRIAGE))).thenReturn(response);

		TriageService.Triage triage = this.triageService.triage(ticket);

		assertThat(triage.action()).isEqualTo("auto_close");
		assertThat(triage.decision()).isEqualTo(Decision.ESCALATE);
	}

	@Test
	void troubledTicketByUrgencyRoutesToDepartmentAndExecutes() {
		String ticket = "Server down! Critical outage!";
		SystemOneResponse response = createResponse("technical", 0.85d, 0.90d, 0.30d);
		when(this.typeSafe.systemOne(eq(ticket), eq(TicketQuestions.TRIAGE))).thenReturn(response);

		TriageService.Triage triage = this.triageService.triage(ticket);

		assertThat(triage.action()).isEqualTo("route_to_technical");
		assertThat(triage.department()).isEqualTo("technical");
		assertThat(triage.decision()).isEqualTo(Decision.EXECUTE);
	}

	@Test
	void troubledTicketByFrustrationRoutesToDepartmentAndExecutes() {
		String ticket = "I have been charged twice, fix this immediately!";
		SystemOneResponse response = createResponse("billing", 0.80d, 0.10d, 0.85d);
		when(this.typeSafe.systemOne(eq(ticket), eq(TicketQuestions.TRIAGE))).thenReturn(response);

		TriageService.Triage triage = this.triageService.triage(ticket);

		assertThat(triage.action()).isEqualTo("route_to_billing");
		assertThat(triage.department()).isEqualTo("billing");
		assertThat(triage.decision()).isEqualTo(Decision.EXECUTE);
	}

	@Test
	void troubledTicketWithLowConfidenceEscalates() {
		String ticket = "Need help with something vague and frustrating";
		SystemOneResponse response = createResponse("sales", 0.40d, 0.60d, 0.70d);
		when(this.typeSafe.systemOne(eq(ticket), eq(TicketQuestions.TRIAGE))).thenReturn(response);

		TriageService.Triage triage = this.triageService.triage(ticket);

		assertThat(triage.action()).isEqualTo("route_to_sales");
		assertThat(triage.decision()).isEqualTo(Decision.ESCALATE);
	}

	@ParameterizedTest
	@CsvSource({
			"0.20, 0.40, route_to_sales",
			"0.10, 0.50, route_to_sales",
			"0.20, 0.50, route_to_sales",
			"0.19, 0.49, auto_close"
	})
	void boundaryConditionsForActionDecision(double urgency, double frustration, String expectedAction) {
		String ticket = "Boundary condition test ticket";
		SystemOneResponse response = createResponse("sales", 0.92d, urgency, frustration);
		when(this.typeSafe.systemOne(eq(ticket), eq(TicketQuestions.TRIAGE))).thenReturn(response);

		TriageService.Triage triage = this.triageService.triage(ticket);

		assertThat(triage.action()).isEqualTo(expectedAction);
	}

	private SystemOneResponse createResponse(String department, double confidence, double urgency, double frustration) {
		Map<String, Double> probs = Map.of(department, confidence);
		ChoiceAnswer choice = new ChoiceAnswer(department, probs, confidence);
		NoulAnswer noul = new NoulAnswer(urgency);
		ScoreAnswer score = new ScoreAnswer(frustration, Map.of(), Map.of(), 0.9d);
		Map<String, Answer> answers = Map.of(
				"department", choice,
				"is_urgent", noul,
				"frustration", score
		);
		return new SystemOneResponse("test-model", answers, Usage.EMPTY, "req-1");
	}

}
