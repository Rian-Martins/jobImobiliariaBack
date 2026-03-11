package com.job.job.infrastructure.controller.WahaController;
import com.job.job.application.dto.wahaDto.ChatState;
import com.job.job.application.dto.wahaDto.MessageRequest;
import com.job.job.domain.model.WahaService;
import com.job.job.infrastructure.externalServices.WahaServices.WahaClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import com.job.job.infrastructure.externalServices.WahaServices.*;

import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/waha")
@RequiredArgsConstructor
public class WahaController {

    private final WahaService wahaService;
    private final WahaClient wahaClient;

    /**
     * Endpoint para VOCÊ enviar uma mensagem manual.
     */
    @PostMapping("/send")
    public ResponseEntity<String> enviarMensagem(@RequestBody MessageRequest request) {
        boolean sucesso = wahaClient.enviarMensagem(request.getChatId(), request.getText());
        if (sucesso) {
            return ResponseEntity.ok("Mensagem enviada com sucesso!");
        }
        return ResponseEntity.internalServerError().body("Falha ao enviar mensagem.");
    }

//    /**
//     * Webhook: Onde o WAHA avisa que chegou mensagem nova no WhatsApp.
//     * Configurado no Docker como: http://host.docker.internal:8090/api/waha/webhook
//     */
//    @PostMapping("/webhook")
//    public ResponseEntity<Void> receberWebhook(@RequestBody Map<String, Object> payload) {
//        log.info("--- NOVA MENSAGEM RECEBIDA VIA WEBHOOK ---");
//        log.info("Payload: {}", payload);
//
//        // Aqui você pode extrair o texto e o remetente para processar no 'core'
//        return ResponseEntity.ok().build();
//    }

    @PostMapping("/webhook")
    public ResponseEntity<Void> handleWebhook(@RequestBody Map<String, Object> payload) {
        var data = (Map<String, Object>) payload.get("payload");
        if (data == null || data.get("from") == null) return ResponseEntity.ok().build();

        String from = (String) data.get("from");
        String body = data.get("body") != null ? data.get("body").toString().trim() : "";

        // Ignora mensagens enviadas por você mesmo
        if (Boolean.TRUE.equals(data.get("fromMe"))) return ResponseEntity.ok().build();

        wahaService.processarMensagem(from, body);

        return ResponseEntity.ok().build();
    }
}
