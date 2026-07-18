package ca.yorku.eecs3311.roomscheduler;

import java.math.BigDecimal;
import java.util.Locale;
import java.util.UUID;

/** Strategy pattern: each payment method validates and processes independently. */
public interface PaymentStrategy {
    String method();
    PaymentReceipt pay(BigDecimal amount, String details);

    static PaymentStrategy forMethod(String method) {
        return switch (method.toUpperCase(Locale.ROOT)) {
            case "CREDIT CARD" -> new CreditCardPaymentStrategy();
            case "INSTITUTIONAL BILLING" -> new InstitutionalBillingStrategy();
            case "DEBIT" -> new DebitPaymentStrategy();
            default -> throw new IllegalArgumentException("Unsupported payment method: " + method);
        };
    }
}

record PaymentReceipt(String transactionId, String method, BigDecimal amount) {}

final class CreditCardPaymentStrategy implements PaymentStrategy {
    public String method() { return "Credit Card"; }
    public PaymentReceipt pay(BigDecimal amount, String details) {
        String digits = details == null ? "" : details.replaceAll("\\D", "");
        if (digits.length() < 12) throw new IllegalArgumentException("Enter a valid credit card number.");
        return receipt(amount);
    }
    private PaymentReceipt receipt(BigDecimal amount) {
        return new PaymentReceipt(UUID.randomUUID().toString(), method(), amount);
    }
}

final class InstitutionalBillingStrategy implements PaymentStrategy {
    public String method() { return "Institutional Billing"; }
    public PaymentReceipt pay(BigDecimal amount, String details) {
        if (details == null || !details.trim().matches("[A-Za-z0-9-]{6,20}"))
            throw new IllegalArgumentException("Enter a valid 6–20 character institutional billing ID.");
        return new PaymentReceipt(UUID.randomUUID().toString(), method(), amount);
    }
}

final class DebitPaymentStrategy implements PaymentStrategy {
    public String method() { return "Debit"; }
    public PaymentReceipt pay(BigDecimal amount, String details) {
        String digits = details == null ? "" : details.replaceAll("\\D", "");
        if (digits.length() < 12) throw new IllegalArgumentException("Enter a valid debit card number.");
        return new PaymentReceipt(UUID.randomUUID().toString(), method(), amount);
    }
}
