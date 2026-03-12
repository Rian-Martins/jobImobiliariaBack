package com.job.job.domain.model;

import com.job.job.application.dto.wahaDto.ChatState;
import com.job.job.application.dto.wahaDto.Lead;
import com.job.job.domain.model.wahaEntities.AtendimentoWaha;
import com.job.job.domain.model.wahaEntities.MenuOption;
import com.job.job.domain.util.CpfUtil;
import com.job.job.infrastructure.externalServices.WahaServices.WahaClient;
import com.job.job.infrastructure.externalServices.WahaServices.WahaProperties;
import com.job.job.infrastructure.persistence.ChatStatePersistance.AtendimentoWahaRepository;
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
    private final WahaProperties wahaProperties;
    private final ChatStateRepository chatStateRepository;
    private final LeadRepository leadRepository;
    private final MenuOptionRepository menuOptionRepository;
    private final AtendimentoWahaRepository atendimentoWahaRepository;

    /**
     * Usado para manter o atendimento "Outros" em duas etapas (CPF -> observação).
     * Como o estado do chat é por chatId, guardamos aqui o último atendimento aberto
     * que aguarda observação.
     */
    private final java.util.concurrent.ConcurrentHashMap<String, Long> outrosAguardandoObservacao = new java.util.concurrent.ConcurrentHashMap<>();
    private final java.util.concurrent.ConcurrentHashMap<String, LastInbound> lastInboundByChat = new java.util.concurrent.ConcurrentHashMap<>();
    private static final long INBOUND_DEDUP_WINDOW_MS = 3000L;

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

        String textoNorm = texto == null ? "" : texto.trim();
        if (shouldIgnoreInboundDuplicate(from, textoNorm)) {
            return;
        }

        // Comandos globais (em qualquer etapa)
        String comando = textoNorm.toLowerCase();
        if (comando.equals("sair")) {
            state.setStep("START");
            outrosAguardandoObservacao.remove(from);
            wahaClient.enviarMensagem(from, "Atendimento cancelado. Se precisar, envie *oi* para começar novamente.");
            state.setUpdatedAt(LocalDateTime.now());
            chatStateRepository.save(state);
            return;
        }
        if (comando.equals("voltar") || comando.equals("voltar ao menu inicial") || comando.equals("menu inicial")) {
            state.setStep("MENU_PRINCIPAL");
            wahaClient.enviarMensagem(from, getMenuPrincipalText());
            state.setUpdatedAt(LocalDateTime.now());
            chatStateRepository.save(state);
            return;
        }

        if ("START".equals(state.getStep())) {
            if (ehSaudacao(textoNorm)) {
                wahaClient.enviarMensagem(from, getWelcomeMessage());
                wahaClient.enviarMensagem(from, getMenuPrincipalText());
                state.setStep("MENU_PRINCIPAL");
            } else {
                wahaClient.enviarMensagem(from, "Envie *oi* para começar e ver as opções disponíveis.");
            }
        } else {
            Optional<MenuOption> configuracao = menuOptionRepository
                    .findByCurrentStepAndTriggerText(state.getStep(), textoNorm);

            if (configuracao.isPresent()) {
                MenuOption opt = configuracao.get();
                wahaClient.enviarMensagem(from, opt.getResponseText());
                state.setStep(opt.getNextStep());
            } else {
                processarFluxosDeColeta(from, textoNorm, state);
            }
        }

        state.setUpdatedAt(LocalDateTime.now());
        chatStateRepository.save(state);
    }


    private void enviarMenu(String step, String from, String header) {
        List<MenuOption> opcoes = menuOptionRepository.findAllByCurrentStep(step);
        StringBuilder sb = new StringBuilder(header).append("\n\n");
        for (MenuOption opt : opcoes) {
            sb.append(opt.getTriggerText()).append(". ").append(opt.getMenuTitle()).append("\n");
        }
        wahaClient.enviarMensagem(from, sb.toString());
    }

    private void processarFluxosDeColeta(String from, String texto, ChatState state) {
        switch (state.getStep()) {
            case "MENU_PRINCIPAL":
            case "MENU_ALUGUEL":
            case "MENU_BOLETOS":
                wahaClient.enviarMensagem(from,
                        "Mensagem inválida. Digite o número de uma opção do menu.\n\n" +
                                "Se desejar, digite *Sair* para cancelar ou *Voltar* ao Menu Inicial.");
                break;

            case "ALUGUEL_VER_CASAS_PEDIR_CPF":
                processarCpfFinal(from, texto, state, "ALUGUEL_VER_CASAS",
                        "Recebemos seu CPF. Em instantes vamos te enviar as casas disponíveis para alugar.");
                break;

            case "ALUGUEL_DEBITOS_PEDIR_CPF":
                processarCpfFinal(from, texto, state, "ALUGUEL_DEBITOS",
                        "Recebemos seu CPF. Em instantes vamos te enviar a consulta de débitos.");
                break;

            case "BOLETOS_CPF_PEDIR_CPF":
                processarCpfFinal(from, texto, state, "BOLETOS_CPF",
                        "Recebemos seu CPF. Em instantes vamos te enviar a segunda via do boleto.");
                break;

            case "COMPRAR_PEDIR_CPF":
                processarCpfFinal(from, texto, state, "COMPRAR",
                        "Recebemos seu CPF. Um corretor entrará em contato em breve para te ajudar na compra do imóvel.");
                break;

            case "ANUNCIAR_PEDIR_CPF":
                processarCpfParaAnunciar(from, texto, state);
                break;

            case "ANUNCIAR_OBSERVACAO":
                processarObservacaoAnunciar(from, texto, state);
                break;

            case "OUTROS_PEDIR_CPF":
                processarCpfParaOutros(from, texto, state);
                break;

            case "OUTROS_OBSERVACAO":
                processarObservacaoOutros(from, texto, state);
                break;

            default:
                wahaClient.enviarMensagem(from,
                        "Mensagem inválida.\n\n" +
                                "Se desejar, digite *Sair* para cancelar ou *Voltar* ao Menu Inicial.");
                state.setStep("START");
                break;
        }
    }

    private void processarCpfFinal(String from, String texto, ChatState state, String assunto, String sucessoMsg) {
        String cpfDigits = CpfUtil.onlyDigits(texto);
        if (!CpfUtil.isValid(cpfDigits)) {
            wahaClient.enviarMensagem(from,
                    "CPF inválido. Digite novamente seu *CPF* (somente números).\n\n" +
                            "Se desejar, digite *Sair* para cancelar ou *Voltar* ao Menu Inicial.");
            return;
        }

        AtendimentoWaha at = new AtendimentoWaha();
        at.setChatId(from);
        at.setCpf(cpfDigits);
        at.setAssunto(assunto);
        atendimentoWahaRepository.save(at);

        wahaClient.enviarMensagem(from, sucessoMsg);
        state.setStep("START");
    }

    private void processarCpfParaOutros(String from, String texto, ChatState state) {
        String cpfDigits = CpfUtil.onlyDigits(texto);
        if (!CpfUtil.isValid(cpfDigits)) {
            wahaClient.enviarMensagem(from,
                    "CPF inválido. Digite novamente seu *CPF* (somente números).\n\n" +
                            "Se desejar, digite *Sair* para cancelar ou *Voltar* ao Menu Inicial.");
            return;
        }

        AtendimentoWaha at = new AtendimentoWaha();
        at.setChatId(from);
        at.setCpf(cpfDigits);
        at.setAssunto("OUTROS");
        at = atendimentoWahaRepository.save(at);

        outrosAguardandoObservacao.put(from, at.getId());
        wahaClient.enviarMensagem(from, "Agora, faça sua *observação* (descreva o que você precisa).");
        state.setStep("OUTROS_OBSERVACAO");
    }

    private void processarCpfParaAnunciar(String from, String texto, ChatState state) {
        String cpfDigits = CpfUtil.onlyDigits(texto);
        if (!CpfUtil.isValid(cpfDigits)) {
            wahaClient.enviarMensagem(from,
                    "CPF inválido. Digite novamente seu *CPF* (somente números).\n\n" +
                            "Se desejar, digite *Sair* para cancelar ou *Voltar* ao Menu Inicial.");
            return;
        }

        AtendimentoWaha at = new AtendimentoWaha();
        at.setChatId(from);
        at.setCpf(cpfDigits);
        at.setAssunto("ANUNCIAR");
        at = atendimentoWahaRepository.save(at);

        outrosAguardandoObservacao.put(from, at.getId());
        wahaClient.enviarMensagem(from, "Agora, descreva o *imóvel* que deseja anunciar (endereço, tipo, valor desejado, etc.).");
        state.setStep("ANUNCIAR_OBSERVACAO");
    }

    private void processarObservacaoAnunciar(String from, String texto, ChatState state) {
        Long atendimentoId = outrosAguardandoObservacao.remove(from);
        if (atendimentoId != null) {
            atendimentoWahaRepository.findById(atendimentoId).ifPresent(at -> {
                at.setObservacao(texto);
                atendimentoWahaRepository.save(at);
            });
        }
        wahaClient.enviarMensagem(from, "Obrigado! Recebemos os dados do seu anúncio. Nossa equipe entrará em contato em breve.");
        state.setStep("START");
    }

    private void processarObservacaoOutros(String from, String texto, ChatState state) {
        Long atendimentoId = outrosAguardandoObservacao.remove(from);
        if (atendimentoId != null) {
            atendimentoWahaRepository.findById(atendimentoId).ifPresent(at -> {
                at.setObservacao(texto);
                atendimentoWahaRepository.save(at);
            });
        }

        wahaClient.enviarMensagem(from, "Obrigado! Recebemos sua observação. Um atendente vai analisar e retornar em breve.");
        state.setStep("START");
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

    private boolean ehSaudacao(String texto) {
        if (texto == null) return false;
        String t = texto.trim().toLowerCase();
        if (t.isBlank()) return false;
        return t.equals("oi")
                || t.equals("ola")
                || t.equals("olá")
                || t.equals("bom dia")
                || t.equals("boa tarde")
                || t.equals("boa noite")
                || t.equals("menu")
                || t.equals("iniciar")
                || t.equals("começar")
                || t.equals("comecar");
    }

    private String getWelcomeMessage() {
        String cfg = wahaProperties.getWelcomeMessage();
        if (cfg != null && !cfg.isBlank()) return cfg.trim();
        return "Olá! Bem-vindo(a) à Job Imobiliária. Como posso te ajudar?";
    }

    private String getMenuPrincipalText() {
        String cfg = wahaProperties.getMenuPrincipalText();
        if (cfg != null && !cfg.isBlank()) return cfg.trim();
        return "Escolha uma opção:\n\n1. Aluguel\n2. Boletos\n3. Outros";
    }

    private boolean shouldIgnoreInboundDuplicate(String chatId, String text) {
        long now = System.currentTimeMillis();
        LastInbound prev = lastInboundByChat.get(chatId);
        if (prev != null && prev.text.equals(text) && (now - prev.atMs) <= INBOUND_DEDUP_WINDOW_MS) {
            return true;
        }
        lastInboundByChat.put(chatId, new LastInbound(text, now));
        return false;
    }

    private record LastInbound(String text, long atMs) {}
}
