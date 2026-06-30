package com.payguard.application.cqrs;

/**
 * Bir komutu işleyen handler.
 *
 * @param <C> işlenecek komut tipi
 * @param <R> komutun dönüş tipi
 */
public interface CommandHandler<C extends Command<R>, R> {
    R handle(C command);

    /** Bu handler'ın hangi komut sınıfını işlediğini bildirir (dispatcher eşlemesi için). */
    Class<C> commandType();
}
