package com.fraud.application.scenarios.dto;

import java.util.List;

/** Read model for scenario management endpoints. */
public record ScenarioDto(
        long id,
        String name,
        String productType,
        int module,
        int priority,
        String fraudResponseCode,
        List<RuleDto> rules
) {
    public record RuleDto(String name, String expression) {}
}
