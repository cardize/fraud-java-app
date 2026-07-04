package com.fraud.application.scenarios;

import com.fraud.application.common.ApiResult;
import com.fraud.application.common.PageResult;
import com.fraud.application.cqrs.Command;
import com.fraud.application.scenarios.dto.ScenarioDto;
import com.fraud.domain.shared.ProductType;

/**
 * Paged/filtered scenario listing (any authenticated user).
 *
 * @param productType optional filter (null = all product types)
 * @param module      optional filter (null = all modules)
 */
public record ListScenariosCommand(int page, int size, ProductType productType, Integer module)
        implements Command<ApiResult<PageResult<ScenarioDto>>> {
}
