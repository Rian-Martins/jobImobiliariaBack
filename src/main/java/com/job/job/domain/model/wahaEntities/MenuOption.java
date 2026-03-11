package com.job.job.domain.model.wahaEntities;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import lombok.Data;

@Entity
@Data
public class MenuOption {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String currentStep;  // EX: "MENU_ABERTO"
    private String triggerText;  // EX: "1"
    private String responseText; // EX: "Ótimo! Digite seu nome:"
    private String nextStep;     // EX: "COLETANDO_NOME"
    private String menuTitle;    // Apenas para organização (Ex: "Opção Alugar")
}
