package com.job.job.application.dto.wahaDto;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;

import java.time.LocalDateTime;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import java.time.LocalDateTime;

@Entity
public class ChatState {
    @Id
    private String chatId; // O número do celular (from)
    private String step;   // EX: "MENU", "AGUARDANDO_NOME", "AGUARDANDO_CPF"
    private LocalDateTime updatedAt;

    public ChatState() {}

    public ChatState(String chatId, String step) {
        this.chatId = chatId;
        this.step = step;
        this.updatedAt = LocalDateTime.now();
    }

    // Getters e Setters
    public String getStep() { return step; }
    public void setStep(String step) { this.step = step; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
