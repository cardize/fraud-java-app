package com.payguard.domain.shared;

/**
 * Ürün tipi — hangi senaryo işlemcisinin (processor) seçileceğini belirler.
 *
 * .NET karşılığı: ProductType enum'u (Card, PF, PayCell, TrKart...).
 * Dikey dilimde sadece CARD'ı işliyoruz.
 */
public enum ProductType {
    CARD,
    PF,
    PAYCELL,
    TRKART
}
