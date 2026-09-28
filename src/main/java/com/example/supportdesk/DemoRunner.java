package com.example.supportdesk;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springaicommunity.typesafe.advisor.JevSelfRefineFailedException;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * Triages and drafts a reply for one ticket at startup, logging every number the
 * article quotes. Disable with {@code supportdesk.demo.enabled=false}.
 */
@Component
@ConditionalOnProperty(name = "supportdesk.demo.enabled", havingValue = "true", matchIfMissing = true)
class DemoRunner implements CommandLineRunner {

	private static final Log logger = LogFactory.getLog(DemoRunner.class);

	private final TriageService triage;

	private final ChatClient supportChatClient;

	private final String ticket;

	DemoRunner(TriageService triage, ChatClient supportChatClient, @Value("${supportdesk.demo.ticket}") String ticket) {
		this.triage = triage;
		this.supportChatClient = supportChatClient;
		this.ticket = ticket;
	}

	@Override
	public void run(String... args) {
		TriageService.Triage t = this.triage.triage(this.ticket);
		logger.info("""

				Ticket       : %s
				Model        : %s  (%d ms)
				is_urgent    : %.2f
				department   : %s  confidence %.2f  %s
				frustration  : %.2f
				action       : %s -> %s
				""".formatted(this.ticket, t.model(), t.latencyMs(), t.urgency(), t.department(), t.confidence(),
				t.departmentProbabilities(), t.frustration(), t.action(), t.decision()));

		if (t.decision() == org.springaicommunity.typesafe.judge.JevConfidenceGate.Decision.ESCALATE) {
			logger.info("Escalated to a human; no draft generated.");
			return;
		}
		try {
			long start = System.nanoTime();
			String draft = this.supportChatClient.prompt().user(this.ticket).call().content();
			logger.info("\nApproved draft (%d ms incl. guardrail + judge):\n%s"
				.formatted((System.nanoTime() - start) / 1_000_000, draft));
		}
		catch (JevSelfRefineFailedException ex) {
			logger.warn("Judge rejected every attempt: " + ex.verdict().summary());
		}
	}

}
