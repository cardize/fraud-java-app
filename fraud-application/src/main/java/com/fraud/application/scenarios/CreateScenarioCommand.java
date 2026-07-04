package com.fraud.application.scenarios;

import com.fraud.application.common.ApiResult;
import com.fraud.application.cqrs.Command;
import com.fraud.application.scenarios.dto.ScenarioDto;
import com.fraud.domain.shared.ProductType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

import java.util.List;

/**
 * Command creating a scenario with its rules (ADMIN only — enforced in SecurityConfig).
 * It is both the HTTP request body and the CQRS message.
 *
 * fraudResponseCode is constrained to an UPPER_SNAKE token (e.g. REJECT, REVIEW) — it flows back
 * to clients verbatim, so free-form text is not allowed.
 */
public record CreateScenarioCommand(
        @NotBlank @Size(max = 200) String name,
        @NotNull ProductType productType,
        @Positive int module,
        @PositiveOrZero int priority,
        @NotBlank @Pattern(regexp = "[A-Z][A-Z_]{1,39}") String fraudResponseCode,
        @NotEmpty List<@Valid NewRule> rules
) implements Command<ApiResult<ScenarioDto>> {

    public record NewRule(
            @NotBlank @Size(max = 200) String name,
            @NotBlank @Size(max = 1000) String expression) {}
}
