package kz.kbtu.message_relay;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class MessageRelayApplication {

	public static void main(String[] args) {
		SpringApplication.run(MessageRelayApplication.class, args);
	}

}
