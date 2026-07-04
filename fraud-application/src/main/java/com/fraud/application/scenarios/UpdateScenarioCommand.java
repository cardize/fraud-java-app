package com.fraud.application.scenarios;

import com.fraud.application.common.ApiResult;
import com.fraud.application.cqrs.Command;
import com.fraud.application.scenarios.dto.ScenarioDto;

/**
 * Full-replacement update (PUT semantics): the scenario's fields AND its whole rule list are
 * replaced by the payload. The id comes from the URL path; the payload reuses
 * {@link CreateScenarioCommand}'s validated shape (same constraints, same expression validation).
 */
public record UpdateScenarioCommand(long id, CreateScenarioCommand data)
        implements Command<ApiResult<ScenarioDto>> {
}
