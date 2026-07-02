package com.payguard.application.cqrs;

/**
 * A handler that processes a command.
 *
 * @param <C> the command type handled
 * @param <R> the command's return type
 */
public interface CommandHandler<C extends Command<R>, R> {
    R handle(C command);

    /** Declares which command class this handler processes (used for dispatcher mapping). */
    Class<C> commandType();
}
