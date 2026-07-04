package com.fraud.rules;

import com.fraud.infrastructure.rules.SpelExpressionValidator;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Write-time expression validation: what the admin API accepts must be exactly what the runtime
 * evaluator can execute — including rejecting the SpEL-injection shapes the runtime blocks.
 */
class SpelExpressionValidatorTest {

    private final SpelExpressionValidator validator = new SpelExpressionValidator();

    @Test
    void acceptsPropertyAndOperatorExpressions() {
        assertDoesNotThrow(() -> validator.validate("amountValue > threshold"));
        assertDoesNotThrow(() -> validator.validate("hourOfDay >= 0 and hourOfDay < 6"));
        assertDoesNotThrow(() -> validator.validate("merchantId == 'M1' or amountValue > 1000"));
    }

    @Test
    void rejectsSyntaxError() {
        assertThrows(IllegalArgumentException.class, () -> validator.validate("amountValue >"));
    }

    @Test
    void rejectsInjectionAttemptWithTypeReference() {
        // T(...) type references / method calls are runnable-code injection — the runtime's
        // SimpleEvaluationContext blocks them; the validator must reject them at write time.
        assertThrows(IllegalArgumentException.class,
                () -> validator.validate("T(java.lang.Runtime).getRuntime().exec('calc')"));
    }

    @Test
    void rejectsMethodCall() {
        assertThrows(IllegalArgumentException.class,
                () -> validator.validate("amount.doubleValue() > 100"));
    }

    @Test
    void rejectsUnknownProperty() {
        assertThrows(IllegalArgumentException.class,
                () -> validator.validate("nonExistingField > 5"));
    }

    @Test
    void rejectsNonBooleanResult() {
        assertThrows(IllegalArgumentException.class,
                () -> validator.validate("amountValue + 1"));
    }
}
