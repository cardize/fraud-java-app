package com.fraud.application.scenarios;

import com.fraud.application.common.ApiResult;
import com.fraud.application.cqrs.Command;
import com.fraud.application.scenarios.dto.ScenarioDto;

import java.util.List;

/** Query listing all scenarios with their rules (any authenticated user). */
public record ListScenariosCommand() implements Command<ApiResult<List<ScenarioDto>>> {
}
