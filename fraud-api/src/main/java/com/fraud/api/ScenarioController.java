package com.fraud.api;

import com.fraud.application.common.ApiResult;
import com.fraud.application.cqrs.Mediator;
import com.fraud.application.scenarios.CreateScenarioCommand;
import com.fraud.application.scenarios.DeleteScenarioCommand;
import com.fraud.application.scenarios.ListScenariosCommand;
import com.fraud.application.scenarios.dto.ScenarioDto;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

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

    @GetMapping
    public ApiResult<List<ScenarioDto>> list() {
        return mediator.send(new ListScenariosCommand());
    }

    @PostMapping
    public ApiResult<ScenarioDto> create(@Valid @RequestBody CreateScenarioCommand command) {
        return mediator.send(command);
    }

    @DeleteMapping("/{id}")
    public ApiResult<Void> delete(@PathVariable long id) {
        return mediator.send(new DeleteScenarioCommand(id));
    }
}
