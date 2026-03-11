package com.job.job.infrastructure.externalServices.WahaServices;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = "waha")
public class WahaProperties {

    private boolean enabled = false;
    private String baseUrl;
    private String session;
    private long intervaloEnvioMs;
    private String mensagemNovaPergunta;
    private String apiKey;

}