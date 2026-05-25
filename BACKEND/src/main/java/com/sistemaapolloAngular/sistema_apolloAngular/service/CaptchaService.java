package com.sistemaapolloAngular.sistema_apolloAngular.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

@Service
public class CaptchaService {

    private static final String RECAPTCHA_VERIFY_URL = "https://www.google.com/recaptcha/api/siteverify";

    @Value("${google.recaptcha.secret:}")
    private String secretKey;

    @Value("${google.recaptcha.site:}")
    private String siteKey;

    @Value("${google.recaptcha.threshold:0.5}")
    private float threshold;

    public boolean validateCaptchaV3(String captchaResponse, String remoteIp) {
        try {
            System.out.println(" === VALIDACIÓN reCAPTCHA INICIADA ===");
            System.out.println(" Secret Key configurada: " + (secretKey != null && !secretKey.isEmpty() ? "SÍ" : "NO"));
            System.out.println(" Site Key configurada: " + (siteKey != null && !siteKey.isEmpty() ? "SÍ" : "NO"));
            System.out.println(" Threshold: " + threshold);
            System.out.println(" Longitud del token recibido: " + (captchaResponse != null ? captchaResponse.length() : 0));
            System.out.println(" Token (primeros 50 chars): " +
                    (captchaResponse != null && captchaResponse.length() > 50 ?
                            captchaResponse.substring(0, 50) + "..." : captchaResponse));

            // Validación mejorada del token
            if (captchaResponse == null || captchaResponse.trim().isEmpty() || captchaResponse.length() < 10) {
                System.out.println("ERROR: reCAPTCHA response está vacío o es demasiado corto");
                return false;
            }

            if (secretKey == null || secretKey.trim().isEmpty()) {
                System.out.println(" ERROR: Secret Key no configurada en application.properties");
                System.out.println(" Verifica que tengas: google.recaptcha.secret=tu_secret_key");
                return false;
            }

            // Construir parámetros para el POST
            MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
            params.add("secret", secretKey);
            params.add("response", captchaResponse);
            if (remoteIp != null) {
                params.add("remoteip", remoteIp);
            }

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

            HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(params, headers);

            System.out.println(" Enviando verificación a Google...");

            // Hacer la llamada a Google
            RestTemplate restTemplate = new RestTemplate();
            RecaptchaResponse recaptchaResponse =
                    restTemplate.postForObject(RECAPTCHA_VERIFY_URL, request, RecaptchaResponse.class);

            if (recaptchaResponse == null) {
                System.out.println(" Respuesta nula de Google");
                return false;
            }

            System.out.println(" Success de Google: " + recaptchaResponse.isSuccess());
            System.out.println(" Score de Google: " + recaptchaResponse.getScore());
            System.out.println(" Action de Google: " + recaptchaResponse.getAction());

            if (recaptchaResponse.getErrorCodes() != null && !recaptchaResponse.getErrorCodes().isEmpty()) {
                System.out.println(" Errores de Google:");
                recaptchaResponse.getErrorCodes().forEach(err -> System.out.println("   - " + err));
            }

            // Validación para reCAPTCHA v3
            if (recaptchaResponse.isSuccess() && recaptchaResponse.getScore() >= threshold) {
                System.out.println(" reCAPTCHA v3 válido con score: " + recaptchaResponse.getScore());
                return true;
            } else {
                System.out.println(" reCAPTCHA v3 inválido. Score: " + recaptchaResponse.getScore() +
                        ", Success: " + recaptchaResponse.isSuccess() +
                        ", Threshold requerido: " + threshold);
                return false;
            }

        } catch (Exception e) {
            System.out.println(" Error validando reCAPTCHA: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    public String getSiteKey() {
        return siteKey;
    }
}
