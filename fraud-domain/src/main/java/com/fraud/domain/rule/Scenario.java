package com.fraud.domain.rule;

import java.io.Serializable;
import java.util.List;

/**
 * A scenario: an ordered/prioritized set of rules and the fraud response code to return when it fires.
 *
 * @param id              scenario identifier
 * @param name            name
 * @param priority        priority (lower = evaluated first; if multiple hit, the highest-priority one wins)
 * @param fraudResponseCode  code returned when the scenario fires (e.g. "REJECT", "REVIEW")
 * @param rules           the scenario's rules (the scenario "hits" only if all of them are true)
 */
public record Scenario(
        long id,
        String name,
        int priority,
        String fraudResponseCode,
        List<Rule> rules
) implements Serializable {
}
