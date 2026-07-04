package com.fraud.application.scenarios;

import com.fraud.application.common.ApiResult;
import com.fraud.application.cqrs.CommandHandler;
import com.fraud.application.scenarios.dto.ScenarioDto;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Component
public class ListScenariosHandler
        implements CommandHandler<ListScenariosCommand, ApiResult<List<ScenarioDto>>> {

    private final ScenarioAdminStore store;

    public ListScenariosHandler(ScenarioAdminStore store) {
        this.store = store;
    }

    @Override
    @Transactional(readOnly = true)
    public ApiResult<List<ScenarioDto>> handle(ListScenariosCommand cmd) {
        return ApiResult.ok(store.list());
    }

    @Override
    public Class<ListScenariosCommand> commandType() {
        return ListScenariosCommand.class;
    }
}
