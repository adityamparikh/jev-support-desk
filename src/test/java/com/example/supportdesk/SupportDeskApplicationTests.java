package com.example.supportdesk;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class SupportDeskApplicationTests {

	@Autowired
	private ApplicationContext context;

	@Test
	void contextLoads() {
		assertThat(this.context.containsBean("supportDeskApplication")).isTrue();
		assertThat(this.context.containsBean("triageService")).isTrue();
		assertThat(this.context.containsBean("ticketController")).isTrue();
		assertThat(this.context.containsBean("supportReplyJudge")).isTrue();
		assertThat(this.context.containsBean("supportChatClient")).isTrue();
		assertThat(this.context.containsBean("demoRunner")).isFalse();
	}

}
