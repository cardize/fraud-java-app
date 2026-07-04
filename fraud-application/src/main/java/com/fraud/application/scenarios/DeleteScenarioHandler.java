package com.fraud.application.scenarios;

import com.fraud.application.audit.AuditTrail;
import com.fraud.application.common.ApiResult;
import com.fraud.application.cqrs.CommandHandler;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class DeleteScenarioHandler implements CommandHandler<DeleteScenarioCommand, ApiResult<Void>> {

    private final ScenarioAdminStore store;
    private final AuditTrail audit;

    public DeleteScenarioHandler(ScenarioAdminStore store, AuditTrail audit) {
        this.store = store;
        this.audit = audit;
    }

    @Override
    @Transactional
    public ApiResult<Void> handle(DeleteScenarioCommand cmd) {
        if (!store.delete(cmd.id())) {
            return ApiResult.fail("Scenario not found: " + cmd.id());
        }
        audit.record("SCENARIO_DELETED", "id=" + cmd.id());
        return ApiResult.ok(null, "Scenario deleted");
    }

    @Override
    public Class<DeleteScenarioCommand> commandType() {
        return DeleteScenarioCommand.class;
    }
}
