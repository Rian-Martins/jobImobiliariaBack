package com.job.job.infrastructure.externalServices.WahaServices;

import org.springframework.beans.factory.annotation.Autowired;

public final class WahaChatIdUtil {
    private WahaChatIdUtil() {}

    public static String normalizeToCanonicalChatId(String input) {
        if (input == null || input.isBlank()) {
            return null;
        }

        // Remove tudo que não for número
        String digits = input.replace("@c.us", "").replaceAll("\\D", "");

        if (digits.isEmpty()) {
            return null;
        }

        // Se a pessoa digitou com um 0 no início do DDD (ex: 061999999999), removemos esse 0
        if (digits.startsWith("0")) {
            digits = digits.substring(1);
        }

        // Remove temporariamente o "55" se já vier junto, para facilitar a análise do número nacional
        boolean has55 = false;
        if (digits.length() > 11 && digits.startsWith("55")) {
            digits = digits.substring(2);
            has55 = true;
        }

        // === REGRA DO 9º DÍGITO DO WHATSAPP NO BRASIL ===
        // Se o número nacional tiver 11 dígitos e começar com '9' logo após o DDD
        if (digits.length() == 11 && digits.charAt(2) == '9') {
            int ddd = Integer.parseInt(digits.substring(0, 2));

            // O WhatsApp só usa o 9º dígito em DDDs até 27 (SP, RJ, ES, etc.)
            // Para DDDs acima de 27 (ex: Goiás 61, Bahia 62, etc.), retiramos o 9 extra
            if (ddd > 27) {
                // Pega os 2 dígitos do DDD e junta com o resto, pulando o '9' (que está na posição 2)
                digits = digits.substring(0, 2) + digits.substring(3);
            }
        }

        // Recoloca o DDI do Brasil (55)
        if (digits.length() == 10 || digits.length() == 11) {
            digits = "55" + digits;
        } else if (has55) {
            digits = "55" + digits;
        }

        // Retorna o formato final exigido pelo WAHA
        return digits + "@c.us";
    }

}
