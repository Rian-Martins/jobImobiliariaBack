package com.job.job.infrastructure.config;

import com.job.job.domain.model.wahaEntities.MenuOption;
import com.job.job.infrastructure.persistence.ChatStatePersistance.MenuOptionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class WahaMenuInitializer implements ApplicationRunner {

    private final MenuOptionRepository menuOptionRepository;

    @Override
    public void run(ApplicationArguments args) {
        ensureOptions(List.of(
                option("MENU_PRINCIPAL", "1", "Quero comprar um imóvel",
                        "Ótimo! Para te ajudar a *comprar* um imóvel, informe seu *CPF* (somente números).",
                        "COMPRAR_PEDIR_CPF"),
                option("MENU_PRINCIPAL", "2", "Quero alugar um imóvel",
                        "Perfeito. Escolha uma opção de *Aluguel*:\n\n1. Verificar Casas Disponíveis para alugar\n2. Consultar meus Débitos",
                        "MENU_ALUGUEL"),
                option("MENU_PRINCIPAL", "3", "Quero anunciar meu imóvel",
                        "Para *anunciar* seu imóvel, informe seu *CPF* (somente números).",
                        "ANUNCIAR_PEDIR_CPF"),
                option("MENU_PRINCIPAL", "4", "Financeiro / 2ª via de boleto",
                        "Perfeito. Escolha uma opção *Financeira*:\n\n1. Boletos do CPF",
                        "MENU_BOLETOS"),
                option("MENU_PRINCIPAL", "5", "Outras dúvidas / Falar com um corretor",
                        "Certo. Para seguirmos, informe seu *CPF* (somente números).",
                        "OUTROS_PEDIR_CPF")
        ));

        ensureOptions(List.of(
                option("MENU_ALUGUEL", "1", "Verificar Casas Disponíveis para alugar",
                        "Para consultar casas disponíveis, informe seu *CPF* (somente números).",
                        "ALUGUEL_VER_CASAS_PEDIR_CPF"),
                option("MENU_ALUGUEL", "2", "Consultar meus Débitos",
                        "Para consultar débitos, informe seu *CPF* (somente números).",
                        "ALUGUEL_DEBITOS_PEDIR_CPF")
        ));

        ensureOptions(List.of(
                option("MENU_BOLETOS", "1", "Boletos do CPF",
                        "Para emitir/consultar boletos, informe seu *CPF* (somente números).",
                        "BOLETOS_CPF_PEDIR_CPF")
        ));
    }

    /**
     * Garante que as opções existam mesmo que o banco já tenha algumas (evita faltar "3. Outros").
     */
    private void ensureOptions(List<MenuOption> desired) {
        for (MenuOption want : desired) {
            MenuOption opt = menuOptionRepository
                    .findByCurrentStepAndTriggerText(want.getCurrentStep(), want.getTriggerText())
                    .orElseGet(MenuOption::new);

            opt.setCurrentStep(want.getCurrentStep());
            opt.setTriggerText(want.getTriggerText());
            opt.setMenuTitle(want.getMenuTitle());
            opt.setResponseText(want.getResponseText());
            opt.setNextStep(want.getNextStep());

            menuOptionRepository.save(opt);
        }
    }

    private MenuOption option(String currentStep, String triggerText, String title, String response, String nextStep) {
        MenuOption o = new MenuOption();
        o.setCurrentStep(currentStep);
        o.setTriggerText(triggerText);
        o.setMenuTitle(title);
        o.setResponseText(response);
        o.setNextStep(nextStep);
        return o;
    }
}

