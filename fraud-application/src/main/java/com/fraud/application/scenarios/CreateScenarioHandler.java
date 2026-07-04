package com.fraud.application.scenarios;

import com.fraud.application.common.ApiResult;
import com.fraud.application.cqrs.CommandHandler;
import com.fraud.application.scenarios.dto.ScenarioDto;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Creates a scenario after validating every rule expression at WRITE time.
 *
 * An invalid expression is a user error, not a system failure — it comes back as a failed
 * ApiResult naming the offending rule, and NOTHING is persisted (all-or-nothing: one bad rule
 * rejects the whole scenario).
 */
@Component
public class CreateScenarioHandler
        implements CommandHandler<CreateScenarioCommand, ApiResult<ScenarioDto>> {

    private final ScenarioAdminStore store;
    private final ExpressionValidator expressionValidator;

    public CreateScenarioHandler(ScenarioAdminStore store, ExpressionValidator expressionValidator) {
        this.store = store;
        this.expressionValidator = expressionValidator;
    }

    @Override
    @Transactional
    public ApiResult<ScenarioDto> handle(CreateScenarioCommand cmd) {
        for (CreateScenarioCommand.NewRule rule : cmd.rules()) {
            try {
                expressionValidator.validate(rule.expression());
            } catch (IllegalArgumentException e) {
                return ApiResult.fail("Invalid expression in rule '" + rule.name() + "': " + e.getMessage());
            }
        }
        return ApiResult.ok(store.create(cmd), "Scenario created");
    }

    @Override
    public Class<CreateScenarioCommand> commandType() {
        return CreateScenarioCommand.class;
    }
}
