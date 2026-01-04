package com.planify.notification.service;

import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;
import org.thymeleaf.templatemode.TemplateMode;
import org.thymeleaf.templateresolver.StringTemplateResolver;

import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class TemplateService {

    private final TemplateEngine templateEngine;

    public TemplateService() {
        StringTemplateResolver templateResolver = new StringTemplateResolver();
        templateResolver.setTemplateMode(TemplateMode.HTML);
        templateResolver.setCacheable(false);

        this.templateEngine = new SpringTemplateEngine();
        this.templateEngine.setTemplateResolver(templateResolver);
    }

    public String renderTemplate(String templateString, Map<String, Object> variables) {
        Context context = new Context();
        if (variables != null) {
            context.setVariables(variables);
        }

        // Pretvorimo ${variable} sintakso v Thymeleaf [[${variable}]] sintakso
        String thymeleafBody = convertToThymeleafInline(templateString);

        // Omogočimo Thymeleaf inline text evalvacjijo, tako da ovijemo vsebino
        String thymeleafTemplate = "<div th:inline=\"text\">" + thymeleafBody + "</div>";

        return templateEngine.process(thymeleafTemplate, context);
    }

    public String renderSmsTemplate(String templateString, Map<String, Object> variables) {
        String result = templateString != null ? templateString : "";

        // Zamenjava spremenljivk za SMS
        if (variables != null && !variables.isEmpty()) {
            for (Map.Entry<String, Object> entry : variables.entrySet()) {
                String placeholder = "${" + entry.getKey() + "}";
                String value = entry.getValue() != null ? entry.getValue().toString() : "";
                result = result.replace(placeholder, value);
            }
        }

        return result;
    }

    private static final Pattern VAR_PATTERN = Pattern.compile("\\$\\$\\{([^}]+)}");

    private String convertToThymeleafInline(String input) {
        if (input == null || input. isEmpty()) {
            return input;
        }

        // Najprej zamenjamo spremenljivke v atributih (href, src, etc.)
        String result = input. replaceAll(
                "((\\? : href|src|action|data-[\\w-]+)\\s*=\\s*\")\\$\\$\\{([^}]+)\\}\"",
                "$1[(${$2})]]\""
        );

        // Nato zamenjamo spremenljivke v besedilu vsebine
        result = result.replaceAll("\\$\\$\\{([^}]+)\\}", "[[\\${$1}]]");

        return result;
    }
}
