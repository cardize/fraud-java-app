package com.fraud.application.common;

import java.util.List;

/**
 * Framework-independent page envelope.
 *
 * The application layer cannot use Spring Data's Page/Pageable (spring-data is an infrastructure
 * dependency); this record carries the same information across the port boundary.
 */
public record PageResult<T>(
        List<T> items,
        int page,
        int size,
        long totalItems,
        int totalPages
) {
}
