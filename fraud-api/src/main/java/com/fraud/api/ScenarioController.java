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
public class ScenarioController {

    private final Mediator mediator;

    public ScenarioController(Mediator mediator) {
        this.mediator = mediator;
    }

    /** Paged listing; productType/module are optional filters. Size is clamped server-side (max 100). */
    @GetMapping
    public ApiResult<PageResult<ScenarioDto>> list(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) ProductType productType,
            @RequestParam(required = false) Integer module) {
        return mediator.send(new ListScenariosCommand(page, size, productType, module));
    }

    @PostMapping
    public ApiResult<ScenarioDto> create(@Valid @RequestBody CreateScenarioCommand command) {
        return mediator.send(command);
    }

    /** Full replacement (PUT): the body reuses the create shape; the id comes from the path. */
    @PutMapping("/{id}")
    public ApiResult<ScenarioDto> update(@PathVariable long id,
                                         @Valid @RequestBody CreateScenarioCommand body) {
        return mediator.send(new UpdateScenarioCommand(id, body));
    }

    @DeleteMapping("/{id}")
    public ApiResult<Void> delete(@PathVariable long id) {
        return mediator.send(new DeleteScenarioCommand(id));
    }
}
