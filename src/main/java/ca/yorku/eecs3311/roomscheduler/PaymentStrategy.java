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
            case "CAMPUS CARD" -> new CampusCardPaymentStrategy();
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

final class CampusCardPaymentStrategy implements PaymentStrategy {
    public String method() { return "Campus Card"; }
    public PaymentReceipt pay(BigDecimal amount, String details) {
        if (details == null || !details.trim().matches("\\d{9}"))
            throw new IllegalArgumentException("Campus card number must contain 9 digits.");
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

