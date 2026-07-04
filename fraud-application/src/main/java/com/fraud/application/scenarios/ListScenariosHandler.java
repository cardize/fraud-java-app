package com.fraud.application.scenarios;

import com.fraud.application.common.ApiResult;
import com.fraud.application.common.PageResult;
import com.fraud.application.cqrs.CommandHandler;
import com.fraud.application.scenarios.dto.ScenarioDto;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class ListScenariosHandler
        implements CommandHandler<ListScenariosCommand, ApiResult<PageResult<ScenarioDto>>> {

    /** Upper bound on page size — an unbounded ?size= would let one request load the whole table. */
    private static final int MAX_PAGE_SIZE = 100;

    private final ScenarioAdminStore store;

    public ListScenariosHandler(ScenarioAdminStore store) {
        this.store = store;
    }

    @Override
    @Transactional(readOnly = true)
    public ApiResult<PageResult<ScenarioDto>> handle(ListScenariosCommand cmd) {
        // Sanitize instead of rejecting: out-of-range paging values are a UX nuisance, not an
        // attack — clamp them and serve the request.
        int page = Math.max(0, cmd.page());
        int size = Math.min(MAX_PAGE_SIZE, Math.max(1, cmd.size()));
        return ApiResult.ok(store.list(new ListScenariosCommand(page, size, cmd.productType(), cmd.module())));
    }

    @Override
    public Class<ListScenariosCommand> commandType() {
        return ListScenariosCommand.class;
    }
}
