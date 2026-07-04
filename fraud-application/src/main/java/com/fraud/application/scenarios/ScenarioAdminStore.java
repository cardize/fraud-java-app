package com.fraud.application.scenarios;

import com.fraud.application.common.PageResult;
import com.fraud.application.scenarios.dto.ScenarioDto;

import java.util.Optional;

/**
 * PORT: scenario management writes/reads (create/delete/list).
 *
 * The adapter (JpaScenarioAdminStore in infrastructure) is also responsible for evicting the
 * scenario cache after every mutation — a stale cache would keep serving the OLD rule set for up
 * to its TTL, so eviction must be tied to the mutation itself, not left to the caller.
 */
public interface ScenarioAdminStore {

    ScenarioDto create(CreateScenarioCommand command);

    /** Full replacement (PUT semantics); empty when no scenario with the given id exists. */
    Optional<ScenarioDto> update(long id, CreateScenarioCommand data);

    /** @return false when no scenario with the given id exists */
    boolean delete(long id);

    /** Paged listing with optional productType/module filters (see ListScenariosCommand). */
    PageResult<ScenarioDto> list(ListScenariosCommand query);
}
