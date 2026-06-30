package com.payguard.domain.shared;

/**
 * İşlem kontrol kodu. Duplicate işlemler için fraud kontrolü atlanır.
 */
public enum ControlCode {
    NORMAL,
    DUPLICATE
}
