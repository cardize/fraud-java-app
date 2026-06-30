package com.payguard.application.cqrs;

import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Komutu uygun handler'a yönlendiren basit aracı (dispatcher).
 *
 * Controller send(command) çağırır; doğru handler otomatik bulunur. Spring tüm CommandHandler
 * bean'lerini constructor'a enjekte eder; komut tipi -> handler eşlemesi bir map'te tutulur.
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
                    "Komut için handler bulunamadı: " + command.getClass().getName());
        }
        return handler.handle(command);
    }
}
