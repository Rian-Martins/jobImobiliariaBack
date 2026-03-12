package com.job.job.domain.model.wahaEntities;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import lombok.Data;

import java.time.LocalDateTime;

@Entity
@Data
public class AtendimentoWaha {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Número/chatId vindo do WAHA (payload.from).
     */
    private String chatId;

    /**
     * CPF normalizado (apenas dígitos).
     */
    private String cpf;

    /**
     * Rota/assunto do atendimento (ex.: ALUGUEL_VER_CASAS, BOLETOS_CPF, OUTROS).
     */
    private String assunto;

    /**
     * Observação livre (usado no fluxo "Outros").
     */
    private String observacao;

    private LocalDateTime createdAt = LocalDateTime.now();
}

