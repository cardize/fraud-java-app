package com.payguard.infrastructure.persistence;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

/**
 * Atomik mesaj-claim satırı: race-free duplicate tespiti için kullanılır.
 *
 * Aynı (messageId, module) çiftine ait İKİNCİ insert denemesi, DB'deki UNIQUE constraint
 * tarafından reddedilir. Bu, "önce SELECT ile kontrol et, sonra INSERT et" deseninin yarış
 * koşuluna (TOCTOU) açık olmasının yerini alır — INSERT'in kendisi atomik mutex görevi görür.
 */
@Entity
@Table(name = "message_claims", uniqueConstraints = @UniqueConstraint(columnNames = {"message_id", "module"}))
public class MessageClaim {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private long messageId;
    private int module;

    protected MessageClaim() {
    }

    public MessageClaim(long messageId, int module) {
        this.messageId = messageId;
        this.module = module;
    }
}
