package com.example.supportdesk;

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

	private final ReplyDrafter drafter;

	TicketController(TriageService triage, ReplyDrafter drafter) {
		this.triage = triage;
		this.drafter = drafter;
	}

	@PostMapping(consumes = MediaType.TEXT_PLAIN_VALUE)
	TicketResult submit(@RequestBody String ticket) {
		TriageService.Triage t = this.triage.triage(ticket);
		String draft = switch (t.decision()) {
			case EXECUTE, CONFIRM -> this.drafter.draft(ticket);
			case ESCALATE -> null; // a human writes this one
		};
		return new TicketResult(t, draft);
	}

	@ExceptionHandler(ReplyRejectedException.class)
	ResponseEntity<String> judgeGaveUp(ReplyRejectedException e) {
		return ResponseEntity.accepted().body("Queued for a human: " + e.verdict().summary());
	}

	record TicketResult(TriageService.Triage triage, String draft) {
	}

}
