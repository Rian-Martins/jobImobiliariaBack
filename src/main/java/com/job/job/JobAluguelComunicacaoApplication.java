package com.job.job; // O pacote deve refletir a pasta real

import com.job.job.infrastructure.externalServices.WahaServices.WahaProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableConfigurationProperties(WahaProperties.class)
public class JobAluguelComunicacaoApplication {

	public static void main(String[] args) {
		SpringApplication.run(JobAluguelComunicacaoApplication.class, args);
	}
}