package com.payguard.application.cqrs;

import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Komutu uygun handler'a yönlendiren basit aracı (dispatcher).
 *
 * .NET karşılığı: MediatR'ın IMediator.Send(command) mekanizması.
 * Controller "Mediator.Send(command)" çağırır; doğru handler otomatik bulunur.
 *
 * Spring tüm CommandHandler bean'lerini constructor'a enjekte eder (DI),
 * biz de komut tipi -> handler eşlemesini bir map'te tutarız.
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
