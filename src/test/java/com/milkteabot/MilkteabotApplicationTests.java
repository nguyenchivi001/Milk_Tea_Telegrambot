package com.milkteabot;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.telegram.telegrambots.meta.TelegramBotsApi;

@SpringBootTest
class MilkteabotApplicationTests {

	@MockBean
	TelegramBotsApi telegramBotsApi;

	@Test
	void contextLoads() {
	}

}
