package com.smartroomfinder.smartroomfinder;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import java.util.Base64;

@SpringBootApplication
public class SmartRoomFinderApplication {

	public static void main(String[] args) {

		SpringApplication.run(SmartRoomFinderApplication.class, args);
		byte[] key = Keys.secretKeyFor(SignatureAlgorithm.HS512).getEncoded();
		System.out.println(Base64.getEncoder().encodeToString(key));
	}

}
