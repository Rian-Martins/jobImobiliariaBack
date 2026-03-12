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
    /**
     * Mensagem de boas-vindas enviada quando o usuário inicia (ex.: manda "oi").
     */
    private String welcomeMessage;
    /**
     * Texto completo do menu principal (opções 1 a 5). Enviado logo após a boas-vindas.
     */
    private String menuPrincipalText;
    private String mensagemNovaPergunta;
    private String apiKey;

}