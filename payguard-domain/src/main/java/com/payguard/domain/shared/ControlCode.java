package com.payguard.domain.shared;

/**
 * İşlem kontrol kodu.
 *
 * .NET karşılığı: Domain.Shared/Enumerations içindeki ControlCode enum'u.
 * Duplicate işlemler için fraud kontrolü atlanır (bkz. handler akışı).
 */
public enum ControlCode {
    NORMAL,
    DUPLICATE
}
