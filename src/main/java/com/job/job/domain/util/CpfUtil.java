package com.job.job.domain.util;

public final class CpfUtil {
    private CpfUtil() {}

    public static String onlyDigits(String value) {
        if (value == null) return "";
        return value.replaceAll("\\D+", "");
    }

    public static boolean isValid(String cpfRaw) {
        String cpf = onlyDigits(cpfRaw);
        if (cpf.length() != 11) return false;
        if (cpf.chars().distinct().count() == 1) return false; // ex: 00000000000

        int dv1 = calcDigit(cpf, 9, 10);
        int dv2 = calcDigit(cpf, 10, 11);
        return dv1 == (cpf.charAt(9) - '0') && dv2 == (cpf.charAt(10) - '0');
    }

    private static int calcDigit(String cpf, int length, int weightStart) {
        int sum = 0;
        int weight = weightStart;
        for (int i = 0; i < length; i++) {
            int num = cpf.charAt(i) - '0';
            sum += num * weight--;
        }
        int mod = sum % 11;
        return (mod < 2) ? 0 : (11 - mod);
    }
}

