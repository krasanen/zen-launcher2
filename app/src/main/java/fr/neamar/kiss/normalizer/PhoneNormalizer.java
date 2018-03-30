package fi.zmengames.zlauncher.normalizer;

import java.util.regex.Pattern;

public class PhoneNormalizer {

    private static final Pattern ignorePattern = Pattern.compile("[-.():/ ]");

    public static String simplifyPhoneNumber(String phoneNumber) {
        return ignorePattern.matcher(phoneNumber).replaceAll("");
    }
}
