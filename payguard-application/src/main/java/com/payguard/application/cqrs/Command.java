package com.payguard.application.cqrs;

/**
 * Bir komut/sorgu mesajı. Dönüş tipini R generic'i taşır.
 *
 * .NET karşılığı: MediatR'ın IRequest<TResponse> arayüzü.
 * (PayGuard'da: RequestGetFraudResponseForCardCommand : IRequest<IDataResult<GetFraudResponseDto>>)
 */
public interface Command<R> {
}
