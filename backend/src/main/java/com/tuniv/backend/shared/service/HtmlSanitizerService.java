package com.tuniv.backend.shared.service;

import org.owasp.html.HtmlPolicyBuilder;
import org.owasp.html.PolicyFactory;
import org.springframework.stereotype.Service;

@Service
public class HtmlSanitizerService {

    private final PolicyFactory policyFactory;

    public HtmlSanitizerService() {
        this.policyFactory = new HtmlPolicyBuilder()
                // Allow basic text formatting
                .allowElements("b", "i", "em", "strong", "u", "br", "p", "div", "span")
                // Allow links but sanitize URLs
                .allowUrlProtocols("https", "http")
                .allowAttributes("href").onElements("a")
                .requireRelNofollowOnLinks()
                // Allow basic styling
                .allowStyling()
                // Allow code blocks
                .allowElements("code", "pre")
                // Build the policy
                .toFactory();
    }

    /**
     * Sanitizes HTML content to prevent XSS attacks
     * @param html the raw HTML input
     * @return sanitized HTML safe for rendering
     */
    public String sanitize(String html) {
        if (html == null || html.trim().isEmpty()) {
            return html;
        }
        
        return policyFactory.sanitize(html);
    }

    /**
     * Sanitizes plain text (converts to HTML entities)
     * @param text the plain text input
     * @return HTML-safe text
     */
    public String sanitizePlainText(String text) {
        if (text == null || text.trim().isEmpty()) {
            return text;
        }
        
        // Convert to HTML entities for maximum safety
        return text.replace("&", "&amp;")
                  .replace("<", "&lt;")
                  .replace(">", "&gt;")
                  .replace("\"", "&quot;")
                  .replace("'", "&#x27;")
                  .replace("/", "&#x2F;");
    }

    /**
     * Sanitizes message body with optional HTML support
     * @param messageBody the message body to sanitize
     * @param allowBasicHtml whether to allow basic HTML formatting
     * @return sanitized message body
     */
    public String sanitizeMessageBody(String messageBody, boolean allowBasicHtml) {
        if (messageBody == null || messageBody.trim().isEmpty()) {
            return messageBody;
        }
        
        if (allowBasicHtml) {
            return sanitize(messageBody);
        } else {
            return sanitizePlainText(messageBody);
        }
    }
}