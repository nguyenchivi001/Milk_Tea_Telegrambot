package com.milkteabot;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Import;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.telegram.telegrambots.starter.TelegramBotStarterConfiguration;

@SpringBootApplication
@Import(TelegramBotStarterConfiguration.class)
@EnableScheduling
public class MilkteabotApplication {

	public static void main(String[] args) {
		SpringApplication.run(MilkteabotApplication.class, args);
	}

}
