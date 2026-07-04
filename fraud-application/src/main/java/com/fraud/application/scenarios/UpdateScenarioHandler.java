package com.fraud.application.scenarios;

import com.fraud.application.audit.AuditTrail;
import com.fraud.application.common.ApiResult;
import com.fraud.application.cqrs.CommandHandler;
import com.fraud.application.scenarios.dto.ScenarioDto;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Replaces a scenario (PUT). Expressions get the same write-time validation as create; nothing is
 * persisted if any rule is invalid, and the audit row commits atomically with the change.
 */
@Component
public class UpdateScenarioHandler
        implements CommandHandler<UpdateScenarioCommand, ApiResult<ScenarioDto>> {

    private final ScenarioAdminStore store;
    private final ExpressionValidator expressionValidator;
    private final AuditTrail audit;

    public UpdateScenarioHandler(ScenarioAdminStore store,
                                 ExpressionValidator expressionValidator,
                                 AuditTrail audit) {
        this.store = store;
        this.expressionValidator = expressionValidator;
        this.audit = audit;
    }

    @Override
    @Transactional
    public ApiResult<ScenarioDto> handle(UpdateScenarioCommand cmd) {
        for (CreateScenarioCommand.NewRule rule : cmd.data().rules()) {
            try {
                expressionValidator.validate(rule.expression());
            } catch (IllegalArgumentException e) {
                return ApiResult.fail("Invalid expression in rule '" + rule.name() + "': " + e.getMessage());
            }
        }
        return store.update(cmd.id(), cmd.data())
                .map(updated -> {
                    audit.record("SCENARIO_UPDATED", "id=" + updated.id() + " name=" + updated.name());
                    return ApiResult.ok(updated, "Scenario updated");
                })
                .orElseGet(() -> ApiResult.fail("Scenario not found: " + cmd.id()));
    }

    @Override
    public Class<UpdateScenarioCommand> commandType() {
        return UpdateScenarioCommand.class;
    }
}
