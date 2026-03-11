package com.job.job.domain.model;

import com.job.job.application.dto.wahaDto.ChatState;
import com.job.job.application.dto.wahaDto.Lead;
import com.job.job.domain.model.wahaEntities.MenuOption;
import com.job.job.infrastructure.externalServices.WahaServices.WahaClient;
import com.job.job.infrastructure.persistence.ChatStatePersistance.ChatStateRepository;
import com.job.job.infrastructure.persistence.ChatStatePersistance.MenuOptionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import com.job.job.infrastructure.persistence.ChatStatePersistance.LeadRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class WahaService {

    private final WahaClient wahaClient;
    private final ChatStateRepository chatStateRepository;
    private final LeadRepository leadRepository;
    private final MenuOptionRepository menuOptionRepository;

//    public void processarMensagem(String from, String texto) {
//        // 1. Busca ou cria o estado
//        ChatState state = chatStateRepository.findById(from)
//                .orElse(new ChatState(from, "START"));
//
//        log.info("Processando mensagem de {}. Estado atual: {}. Texto: {}", from, state.getStep(), texto);
//
//        // 2. Máquina de Estados
//        switch (state.getStep()) {
//            case "START":
//                enviarMenuPrincipal(from);
//                state.setStep("MENU_ABERTO");
//                break;
//
//            case "MENU_ABERTO":
//                handleMenu(from, texto, state);
//                break;
//
//            case "COLETANDO_NOME":
//                salvarNomeLead(from, texto);
//                wahaClient.enviarMensagem(from, "Obrigado, *" + texto + "*! Agora, por favor, digite seu *CPF*:");
//                state.setStep("COLETANDO_CPF");
//                break;
//
//            case "COLETANDO_CPF":
//                salvarCpfLead(from, texto);
//                wahaClient.enviarMensagem(from, "Perfeito! Recebemos seus dados. Um consultor da Job Imobiliária entrará em contato em breve.");
//                state.setStep("START"); // Reinicia para o próximo atendimento
//                break;
//
//            default:
//                enviarMenuPrincipal(from);
//                state.setStep("MENU_ABERTO");
//                break;
//        }
//
//        // 3. Salva a evolução
//        state.setUpdatedAt(LocalDateTime.now());
//        chatStateRepository.save(state);
//    }

    public void processarMensagem(String from, String texto) {
        ChatState state = chatStateRepository.findById(from)
                .orElse(new ChatState(from, "START"));

        // Se for o início, manda o menu principal (que você também pode salvar no banco se quiser)
        if ("START".equals(state.getStep())) {
            enviarMenuPrincipal(from);
            state.setStep("MENU_PRINCIPAL");
        } else {
            // BUSCA DINÂMICA: Procura se existe uma configuração para o passo atual + o que o cara digitou
            Optional<MenuOption> configuracao = menuOptionRepository
                    .findByCurrentStepAndTriggerText(state.getStep(), texto);

            if (configuracao.isPresent()) {
                MenuOption opt = configuracao.get();
                wahaClient.enviarMensagem(from, opt.getResponseText());
                state.setStep(opt.getNextStep());
            } else {
                // Lógica especial para passos de coleta (NOME/CPF) que não são opções de menu
                processarColetaDeDados(from, texto, state);
            }
        }

        state.setUpdatedAt(LocalDateTime.now());
        chatStateRepository.save(state);
    }


    private void enviarMenuPrincipal(String from) {
        List<MenuOption> opcoes = menuOptionRepository.findAllByCurrentStep("MENU_PRINCIPAL");
        StringBuilder sb = new StringBuilder("Olá! Escolha uma opção:\n\n");

        for (MenuOption opt : opcoes) {
            sb.append(opt.getTriggerText()).append(". ").append(opt.getMenuTitle()).append("\n");
        }

        wahaClient.enviarMensagem(from, sb.toString());
    }

    private void handleMenu(String from, String texto, ChatState state) {
        if ("1".equals(texto)) {
            wahaClient.enviarMensagem(from, "Excelente! Para começar o seu cadastro de locação, digite seu *nome completo*:");
            state.setStep("COLETANDO_NOME");
        } else if ("2".equals(texto)) {
            wahaClient.enviarMensagem(from, "Para boletos, acesse nosso portal: jobimobiliaria.com.br/boletos ou digite seu CPF para enviarmos aqui.");
            state.setStep("START");
        } else {
            wahaClient.enviarMensagem(from, "Ops, não entendi. Digite *1* para Alugar ou *2* para Boleto.");
        }
    }

    private void processarColetaDeDados(String from, String texto, ChatState state) {
        switch (state.getStep()) {
            case "COLETANDO_NOME":
                salvarNomeLead(from, texto);
                // Após salvar o nome, você pode buscar no banco qual a próxima instrução
                // OU deixar fixo se for um fluxo padrão de cadastro
                wahaClient.enviarMensagem(from, "Obrigado, *" + texto + "*! Agora, por favor, digite seu *CPF*:");
                state.setStep("COLETANDO_CPF");
                break;

            case "COLETANDO_CPF":
                salvarCpfLead(from, texto);
                wahaClient.enviarMensagem(from, "Perfeito! Recebemos seu CPF. Um consultor entrará em contato em breve.");
                state.setStep("START"); // Volta para o início ou um estado de FINALIZADO
                break;

            default:
                // Se cair aqui, o cara digitou algo que não é opção de menu nem dado esperado
                wahaClient.enviarMensagem(from, "Desculpe, não entendi. Digite *1* para voltar ao menu principal.");
                break;
        }
    }

    private void salvarNomeLead(String from, String nome) {
        Lead lead = leadRepository.findById(from).orElse(new Lead());
        lead.setChatId(from);
        lead.setNome(nome);
        leadRepository.save(lead);
    }

    private void salvarCpfLead(String from, String cpf) {
        leadRepository.findById(from).ifPresent(lead -> {
            lead.setCpf(cpf);
            leadRepository.save(lead);
        });
    }
}
