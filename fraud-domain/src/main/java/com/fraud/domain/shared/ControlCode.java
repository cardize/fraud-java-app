package com.fraud.domain.shared;

/**
 * Transaction control code. Fraud checking is skipped for duplicate transactions.
 */
public enum ControlCode {
    NORMAL,
    DUPLICATE
}
