package com.fraud.application.scenarios;

import com.fraud.application.common.ApiResult;
import com.fraud.application.cqrs.Command;

/** Command deleting a scenario (and, via cascade, its rules). ADMIN only. */
public record DeleteScenarioCommand(long id) implements Command<ApiResult<Void>> {
}
