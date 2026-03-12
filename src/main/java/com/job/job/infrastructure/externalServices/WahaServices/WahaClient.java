package com.job.job.infrastructure.externalServices.WahaServices; // Verifique se este é o seu pacote real

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.Map;

@Component
public class WahaClient {

        private static final Logger log = (Logger) LoggerFactory.getLogger(WahaClient.class);
        private static final String SEND_TEXT_PATH = "/api/sendText";
        private static final long DEFAULT_SEND_INTERVAL_MS = 4000L;
        private static final Object SEND_LOCK = new Object();
        private static long lastSendAtMs = 0L;

        /**
         * Layout fixo alterado para PUBLIC para ser visível por outros serviços.
         * Usamos %s para injeção via String.format().
         */
        public static final String LAYOUT_NOVA_PERGUNTA = "Tudo bem, %s? Aqui é da equipe de suporte do Cadastro PCD de Formosa! 👋\n\n"
                + "Notamos que surgiu uma nova pergunta no seu perfil (%s). Sua participação ajuda a prefeitura "
                + "a entender melhor as necessidades da nossa população 🤝\n\n"
                + "Acesse aqui para responder: https://cadpcd.formosa.go.gov.br/servicos\n\n"
                + "Alguma dúvida? É só nos chamar! Agradecemos muito a sua colaboração! 🙏";

        private final WahaProperties props;
        private final RestTemplate restTemplate;
        private final ObjectMapper objectMapper;

        public WahaClient(WahaProperties props, RestTemplate restTemplate, ObjectMapper objectMapper) {
            this.props = props;
            this.restTemplate = restTemplate;
            this.objectMapper = objectMapper;
        }

        /**
         * Normaliza número de telefone para o formato WAHA.
         */
        public static String normalizarChatId(String phoneNumber) {
            return WahaChatIdUtil.normalizeToCanonicalChatId(phoneNumber);
        }

        /**
         * Envia a notificação de nova pergunta usando o layout padrão.
         */
        public boolean enviarNotificacaoNovaPergunta(String chatId, String nomeDestinatario, String nomeQuestionario) {
            String textoFormatado = String.format(LAYOUT_NOVA_PERGUNTA, nomeDestinatario, nomeQuestionario);
            return enviarMensagem(chatId, textoFormatado);
        }

        /**
         * Envia mensagem de texto genérica com garantia de UTF-8.
         */
        public boolean enviarMensagem(String chatId, String texto) {
            if (!props.isEnabled()) {
                log.debug("WAHA desabilitado, mensagem não enviada para {}", chatId);
                return false;
            }

            try {
                throttleGlobal();

                String baseUrl = props.getBaseUrl();
                String url = baseUrl.replaceAll("/$", "") + SEND_TEXT_PATH;

                Map<String, Object> bodyMap = Map.of(
                        "session", props.getSession() != null ? props.getSession() : "default",
                        "chatId", chatId,
                        "text", texto);

                // ObjectMapper garante que a String Java (Unicode) vire um JSON UTF-8 correto
                String jsonBody = objectMapper.writeValueAsString(bodyMap);

                HttpHeaders headers = new HttpHeaders();
                headers.setContentType(new MediaType(MediaType.APPLICATION_JSON, StandardCharsets.UTF_8));
                headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));

                String apiKey = props.getApiKey();
                if (apiKey != null && !apiKey.isBlank()) {
                    headers.set("X-Api-Key", apiKey);
                }

                HttpEntity<String> entity = new HttpEntity<>(jsonBody, headers);

                log.info("[WAHA DEBUG] Enviando para {}: {}", chatId,
                        texto.substring(0, Math.min(texto.length(), 40)) + "...");

                ResponseEntity<String> response = restTemplate.postForEntity(url, entity, String.class);

                return response.getStatusCode().is2xxSuccessful();
            } catch (Exception e) {
                log.error("[WAHA ERROR] Falha ao enviar mensagem para {}: {}", chatId, e.getMessage());
                return false;
            }
        }

        /**
         * Garante uma fila global de envio com pausa entre mensagens.
         * Isso evita disparos rápidos (e ajuda a reduzir duplicidade percebida no WhatsApp).
         */
        private void throttleGlobal() throws InterruptedException {
            long intervalo = props.getIntervaloEnvioMs() > 0 ? props.getIntervaloEnvioMs() : DEFAULT_SEND_INTERVAL_MS;
            if (intervalo < DEFAULT_SEND_INTERVAL_MS) intervalo = DEFAULT_SEND_INTERVAL_MS;

            synchronized (SEND_LOCK) {
                long now = System.currentTimeMillis();
                long delta = now - lastSendAtMs;
                if (lastSendAtMs > 0 && delta < intervalo) {
                    Thread.sleep(intervalo - delta);
                }
                lastSendAtMs = System.currentTimeMillis();
            }
        }

    }
