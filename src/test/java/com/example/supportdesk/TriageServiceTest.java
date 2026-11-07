package com.example.supportdesk;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import org.springframework.ai.decision.DecisionModel;
import org.springframework.ai.decision.DecisionPrompt;
import org.springframework.ai.decision.DecisionResponse;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class TriageServiceTest {

	private DecisionModel decisionModel;

	private TriageService triageService;

	@BeforeEach
	void setUp() {
		this.decisionModel = Mockito.mock(DecisionModel.class);
		this.triageService = new TriageService(this.decisionModel);
	}

	@Test
	void untroubledTicketWithHighConfidenceAutoClosesAndExecutes() {
		String ticket = "Great product, thank you!";
		DecisionResponse response = createResponse("feedback", 0.95d, 0.05d, 0.10d);
		when(this.decisionModel.call(any(DecisionPrompt.class))).thenReturn(response);

		TriageService.Triage triage = this.triageService.triage(ticket);

		assertThat(triage.action()).isEqualTo("auto_close");
		assertThat(triage.department()).isEqualTo("feedback");
		assertThat(triage.confidence()).isEqualTo(0.95d);
		assertThat(triage.urgency()).isEqualTo(0.05d);
		assertThat(triage.frustration()).isEqualTo(0.10d);
		assertThat(triage.decision()).isEqualTo(ConfidenceGate.Decision.EXECUTE);
		assertThat(triage.model()).isEqualTo("test-model");
		assertThat(triage.latencyMs()).isGreaterThanOrEqualTo(0);
		assertThat(triage.departmentProbabilities()).containsEntry("feedback", 0.95d);

		ArgumentCaptor<DecisionPrompt> prompt = ArgumentCaptor.forClass(DecisionPrompt.class);
		verify(this.decisionModel).call(prompt.capture());
		assertThat(prompt.getValue().getQuestions()).isEqualTo(TicketQuestions.TRIAGE);
	}

	@Test
	void untroubledTicketWithModerateConfidenceConfirms() {
		String ticket = "I like this feature.";
		DecisionResponse response = createResponse("feedback", 0.75d, 0.10d, 0.20d);
		when(this.decisionModel.call(any(DecisionPrompt.class))).thenReturn(response);

		TriageService.Triage triage = this.triageService.triage(ticket);

		assertThat(triage.action()).isEqualTo("auto_close");
		assertThat(triage.decision()).isEqualTo(ConfidenceGate.Decision.CONFIRM);
	}

	@Test
	void untroubledTicketWithLowConfidenceEscalates() {
		String ticket = "Maybe I will use this.";
		DecisionResponse response = createResponse("feedback", 0.50d, 0.15d, 0.25d);
		when(this.decisionModel.call(any(DecisionPrompt.class))).thenReturn(response);

		TriageService.Triage triage = this.triageService.triage(ticket);

		assertThat(triage.action()).isEqualTo("auto_close");
		assertThat(triage.decision()).isEqualTo(ConfidenceGate.Decision.ESCALATE);
	}

	@Test
	void troubledTicketByUrgencyRoutesToDepartmentAndExecutes() {
		String ticket = "Server down! Critical outage!";
		DecisionResponse response = createResponse("technical", 0.85d, 0.90d, 0.30d);
		when(this.decisionModel.call(any(DecisionPrompt.class))).thenReturn(response);

		TriageService.Triage triage = this.triageService.triage(ticket);

		assertThat(triage.action()).isEqualTo("route_to_technical");
		assertThat(triage.department()).isEqualTo("technical");
		assertThat(triage.decision()).isEqualTo(ConfidenceGate.Decision.EXECUTE);
	}

	@Test
	void troubledTicketByFrustrationRoutesToDepartmentAndExecutes() {
		String ticket = "I have been charged twice, fix this immediately!";
		DecisionResponse response = createResponse("billing", 0.80d, 0.10d, 0.85d);
		when(this.decisionModel.call(any(DecisionPrompt.class))).thenReturn(response);

		TriageService.Triage triage = this.triageService.triage(ticket);

		assertThat(triage.action()).isEqualTo("route_to_billing");
		assertThat(triage.department()).isEqualTo("billing");
		assertThat(triage.decision()).isEqualTo(ConfidenceGate.Decision.EXECUTE);
	}

	@Test
	void troubledTicketWithLowConfidenceEscalates() {
		String ticket = "Need help with something vague and frustrating";
		DecisionResponse response = createResponse("sales", 0.40d, 0.60d, 0.70d);
		when(this.decisionModel.call(any(DecisionPrompt.class))).thenReturn(response);

		TriageService.Triage triage = this.triageService.triage(ticket);

		assertThat(triage.action()).isEqualTo("route_to_sales");
		assertThat(triage.decision()).isEqualTo(ConfidenceGate.Decision.ESCALATE);
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
		DecisionResponse response = createResponse("sales", 0.92d, urgency, frustration);
		when(this.decisionModel.call(any(DecisionPrompt.class))).thenReturn(response);

		TriageService.Triage triage = this.triageService.triage(ticket);

		assertThat(triage.action()).isEqualTo(expectedAction);
	}

	// Frustration is given on the 0..1 scale the tests think in; Jev scores it on levels 0..2.
	private DecisionResponse createResponse(String department, double confidence, double urgency,
			double frustration) {
		return TestAnswers.response("test-model", TicketQuestions.TRIAGE)
			.answer("department", TestAnswers.choice(department, confidence))
			.answer("is_urgent", TestAnswers.yesNo(urgency))
			.answer("frustration", TestAnswers.score(frustration * (TicketQuestions.FRUSTRATION_LEVELS - 1)))
			.build();
	}

}
