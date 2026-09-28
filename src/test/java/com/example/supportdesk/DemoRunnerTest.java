package com.example.supportdesk;

import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Answers;
import org.mockito.Mockito;
import org.springaicommunity.typesafe.advisor.JevSelfRefineFailedException;
import org.springaicommunity.typesafe.judge.JevConfidenceGate.Decision;
import org.springaicommunity.typesafe.judge.JevVerdict;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class DemoRunnerTest {

	private TriageService triageService;

	private ChatClient supportChatClient;

	private static final String DEMO_TICKET = "Help! My payouts have been failing for 3 days.";

	private DemoRunner demoRunner;

	@BeforeEach
	void setUp() {
		this.triageService = Mockito.mock(TriageService.class);
		this.supportChatClient = Mockito.mock(ChatClient.class, Answers.RETURNS_DEEP_STUBS);
		this.demoRunner = new DemoRunner(this.triageService, this.supportChatClient, DEMO_TICKET);
	}

	@Test
	void runWhenDecisionIsEscalateDoesNotCallChatClient() {
		TriageService.Triage triage = new TriageService.Triage(
				"route_to_technical",
				"technical",
				0.50d,
				Map.of("technical", 0.50d),
				0.70d,
				0.80d,
				Decision.ESCALATE,
				"jev-model",
				20L
		);

		when(this.triageService.triage(DEMO_TICKET)).thenReturn(triage);

		this.demoRunner.run();

		verify(this.triageService).triage(DEMO_TICKET);
		verifyNoInteractions(this.supportChatClient);
	}

	@Test
	void runWhenDecisionIsExecuteCallsChatClientAndLogsDraft() {
		TriageService.Triage triage = new TriageService.Triage(
				"route_to_billing",
				"billing",
				0.95d,
				Map.of("billing", 0.95d),
				0.80d,
				0.60d,
				Decision.EXECUTE,
				"jev-model",
				35L
		);

		when(this.triageService.triage(DEMO_TICKET)).thenReturn(triage);
		when(this.supportChatClient.prompt().user(DEMO_TICKET).call().content()).thenReturn("Approved reply draft.");

		this.demoRunner.run();

		verify(this.triageService).triage(DEMO_TICKET);
		verify(this.supportChatClient.prompt().user(DEMO_TICKET).call()).content();
	}

	@Test
	void runWhenDecisionIsConfirmCallsChatClient() {
		TriageService.Triage triage = new TriageService.Triage(
				"auto_close",
				"feedback",
				0.75d,
				Map.of("feedback", 0.75d),
				0.10d,
				0.10d,
				Decision.CONFIRM,
				"jev-model",
				30L
		);

		when(this.triageService.triage(DEMO_TICKET)).thenReturn(triage);
		when(this.supportChatClient.prompt().user(DEMO_TICKET).call().content()).thenReturn("Auto-close confirm draft.");

		this.demoRunner.run();

		verify(this.triageService).triage(DEMO_TICKET);
		verify(this.supportChatClient.prompt().user(DEMO_TICKET).call()).content();
	}

	@Test
	void runWhenChatClientThrowsRefineFailedExceptionCatchesGracefully() {
		TriageService.Triage triage = new TriageService.Triage(
				"route_to_billing",
				"billing",
				0.90d,
				Map.of("billing", 0.90d),
				0.80d,
				0.70d,
				Decision.EXECUTE,
				"jev-model",
				40L
		);

		JevVerdict verdict = Mockito.mock(JevVerdict.class);
		when(verdict.summary()).thenReturn("tone was inappropriate");
		JevSelfRefineFailedException exception = new JevSelfRefineFailedException(3, verdict);

		when(this.triageService.triage(DEMO_TICKET)).thenReturn(triage);
		when(this.supportChatClient.prompt().user(DEMO_TICKET).call().content()).thenThrow(exception);

		assertThatCode(() -> this.demoRunner.run()).doesNotThrowAnyException();
		verify(this.triageService).triage(DEMO_TICKET);
	}

	@Test
	void demoRunnerConfiguredWhenEnabledOrMissing() {
		ApplicationContextRunner contextRunner = new ApplicationContextRunner()
			.withUserConfiguration(DemoRunner.class)
			.withBean(TriageService.class, () -> Mockito.mock(TriageService.class))
			.withBean("supportChatClient", ChatClient.class, () -> Mockito.mock(ChatClient.class))
			.withPropertyValues("supportdesk.demo.ticket=test ticket");

		contextRunner.withPropertyValues("supportdesk.demo.enabled=true")
			.run(context -> assertThat(context).hasSingleBean(DemoRunner.class));

		contextRunner
			.run(context -> assertThat(context).hasSingleBean(DemoRunner.class));
	}

	@Test
	void demoRunnerNotConfiguredWhenDisabled() {
		ApplicationContextRunner contextRunner = new ApplicationContextRunner()
			.withUserConfiguration(DemoRunner.class)
			.withBean(TriageService.class, () -> Mockito.mock(TriageService.class))
			.withBean("supportChatClient", ChatClient.class, () -> Mockito.mock(ChatClient.class))
			.withPropertyValues("supportdesk.demo.ticket=test ticket");

		contextRunner.withPropertyValues("supportdesk.demo.enabled=false")
			.run(context -> assertThat(context).doesNotHaveBean(DemoRunner.class));
	}

}
