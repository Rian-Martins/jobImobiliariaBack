package com.job.job.infrastructure.persistence.ChatStatePersistance;

import com.job.job.domain.model.wahaEntities.MenuOption;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.List;

@Repository
public interface MenuOptionRepository extends JpaRepository<MenuOption, Long> {

    // Busca a opção específica que o usuário digitou (Ex: digitou "1" no "MENU_PRINCIPAL")
    Optional<MenuOption> findByCurrentStepAndTriggerText(String currentStep, String triggerText);

    // Busca todas as opções de um determinado passo (Útil para montar o texto do menu automaticamente)
    List<MenuOption> findAllByCurrentStep(String currentStep);
}
