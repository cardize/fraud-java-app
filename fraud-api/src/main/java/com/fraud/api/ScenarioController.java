package com.fraud.api;

import com.fraud.application.common.ApiResult;
import com.fraud.application.common.PageResult;
import com.fraud.application.cqrs.Mediator;
import com.fraud.application.scenarios.CreateScenarioCommand;
import com.fraud.application.scenarios.DeleteScenarioCommand;
import com.fraud.application.scenarios.ListScenariosCommand;
import com.fraud.application.scenarios.UpdateScenarioCommand;
import com.fraud.application.scenarios.dto.ScenarioDto;
import com.fraud.domain.shared.ProductType;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Scenario management endpoints (rule/scenario CRUD).
 *
 * RBAC (SecurityConfig): GET is open to any authenticated user; POST/DELETE mutate the fraud
 * engine's behavior for everyone and require the ADMIN role. Expressions are validated at write
 * time (CreateScenarioHandler + SpelExpressionValidator) and the scenario cache is evicted on
 * every mutation, so changes take effect on the very next transaction.
 */
@RestController
@RequestMapping("/api/v1/scenarios")
@Tag(name = "Scenarios", description = "Rule/scenario CRUD. GET: any authenticated user. "
        + "POST/PUT/DELETE: ADMIN role — they mutate live fraud-decision behavior.")
public class ScenarioController {

    private final Mediator mediator;

    public ScenarioController(Mediator mediator) {
        this.mediator = mediator;
    }

    @GetMapping
    @Operation(summary = "List scenarios", description = "Paged; productType/module are optional "
            + "filters. size is clamped server-side to 100.")
    public ApiResult<PageResult<ScenarioDto>> list(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @Parameter(description = "Optional filter") @RequestParam(required = false) ProductType productType,
            @Parameter(description = "Optional filter") @RequestParam(required = false) Integer module) {
        return mediator.send(new ListScenariosCommand(page, size, productType, module));
    }

    @PostMapping
    @Operation(summary = "Create a scenario", description = "ADMIN only. Every rule expression is "
            + "validated at write time (same locked-down SpEL context the engine evaluates against) "
            + "before anything is persisted; the scenario cache is evicted on success so the change "
            + "affects the very next transaction.")
    public ApiResult<ScenarioDto> create(@Valid @RequestBody CreateScenarioCommand command) {
        return mediator.send(command);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Replace a scenario", description = "ADMIN only. Full replacement (PUT "
            + "semantics) — the body reuses the create shape; the id comes from the path.")
    public ApiResult<ScenarioDto> update(@PathVariable long id,
                                         @Valid @RequestBody CreateScenarioCommand body) {
        return mediator.send(new UpdateScenarioCommand(id, body));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete a scenario", description = "ADMIN only. Cascades to the "
            + "scenario's rules and evicts the scenario cache.")
    public ApiResult<Void> delete(@PathVariable long id) {
        return mediator.send(new DeleteScenarioCommand(id));
    }
}
