package com.fraud.application.scenarios;

import com.fraud.application.common.ApiResult;
import com.fraud.application.cqrs.CommandHandler;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class DeleteScenarioHandler implements CommandHandler<DeleteScenarioCommand, ApiResult<Void>> {

    private final ScenarioAdminStore store;

    public DeleteScenarioHandler(ScenarioAdminStore store) {
        this.store = store;
    }

    @Override
    @Transactional
    public ApiResult<Void> handle(DeleteScenarioCommand cmd) {
        return store.delete(cmd.id())
                ? ApiResult.ok(null, "Scenario deleted")
                : ApiResult.fail("Scenario not found: " + cmd.id());
    }

    @Override
    public Class<DeleteScenarioCommand> commandType() {
        return DeleteScenarioCommand.class;
    }
}
