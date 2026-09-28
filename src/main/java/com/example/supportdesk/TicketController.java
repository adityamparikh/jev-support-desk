package com.example.supportdesk;

import org.springaicommunity.typesafe.advisor.JevSelfRefineFailedException;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/tickets")
class TicketController {

	private final TriageService triage;

	private final ChatClient supportChatClient;

	TicketController(TriageService triage, ChatClient supportChatClient) {
		this.triage = triage;
		this.supportChatClient = supportChatClient;
	}

	@PostMapping(consumes = MediaType.TEXT_PLAIN_VALUE)
	TicketResult submit(@RequestBody String ticket) {
		TriageService.Triage t = this.triage.triage(ticket);
		String draft = switch (t.decision()) {
			case EXECUTE, CONFIRM -> this.supportChatClient.prompt().user(ticket).call().content();
			case ESCALATE -> null; // a human writes this one
		};
		return new TicketResult(t, draft);
	}

	@ExceptionHandler(JevSelfRefineFailedException.class)
	ResponseEntity<String> judgeGaveUp(JevSelfRefineFailedException e) {
		return ResponseEntity.accepted().body("Queued for a human: " + e.verdict().summary());
	}

	record TicketResult(TriageService.Triage triage, String draft) {
	}

}
