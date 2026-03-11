package com.job.job.application.dto.wahaDto;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import lombok.Data;

import java.time.LocalDateTime;

@Entity
@Data
public class Lead {
    @Id
    private String chatId; // Telefone do cliente
    private String nome;
    private String cpf;
    private LocalDateTime dataCadastro = LocalDateTime.now();
}
