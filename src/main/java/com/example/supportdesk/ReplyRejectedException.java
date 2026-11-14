package com.example.supportdesk;

/**
 * Thrown when no draft passed review, or the ticket itself was refused. A human takes over.
 */
class ReplyRejectedException extends RuntimeException {

	private final ReplyJudge.Verdict verdict;

	ReplyRejectedException(ReplyJudge.Verdict verdict) {
		super(verdict.summary());
		this.verdict = verdict;
	}

	ReplyJudge.Verdict verdict() {
		return this.verdict;
	}

}
