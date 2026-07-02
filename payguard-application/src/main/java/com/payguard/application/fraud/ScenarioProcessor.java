package com.payguard.application.fraud;

import com.payguard.domain.shared.ProductType;

/**
 * Scenario processing PORT. There is one implementation per product type (CARD, PF, PayCell...).
 *
 * The port lives in APPLICATION; implementations (using SpEL/DB) live in INFRASTRUCTURE.
 * This means application never knows the rule engine's technical details.
 */
public interface ScenarioProcessor {

    /** The product type this processor supports (used for factory mapping). */
    ProductType supportedType();

    /** Runs the online scenarios and returns the fraud response code. */
    String processOnlineScenarios(int module, FraudParameters params);
}
