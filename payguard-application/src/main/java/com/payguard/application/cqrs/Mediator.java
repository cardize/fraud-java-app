package com.payguard.application.cqrs;

import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Simple dispatcher that routes a command to the right handler.
 *
 * The controller calls send(command); the correct handler is found automatically. Spring injects
 * every CommandHandler bean into the constructor; the command type -> handler mapping is kept in
 * a map.
 */
@Component
public class Mediator {

    private final Map<Class<?>, CommandHandler<?, ?>> handlers = new HashMap<>();

    public Mediator(List<CommandHandler<?, ?>> handlerBeans) {
        for (CommandHandler<?, ?> handler : handlerBeans) {
            handlers.put(handler.commandType(), handler);
        }
    }

    @SuppressWarnings("unchecked")
    public <R> R send(Command<R> command) {
        CommandHandler<Command<R>, R> handler =
                (CommandHandler<Command<R>, R>) handlers.get(command.getClass());
        if (handler == null) {
            throw new IllegalStateException(
                    "No handler found for command: " + command.getClass().getName());
        }
        return handler.handle(command);
    }
}
