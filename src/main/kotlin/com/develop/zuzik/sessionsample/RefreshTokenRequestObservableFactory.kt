package com.develop.zuzik.sessionsample

import rx.Observable
import rx.Scheduler
import java.util.concurrent.TimeUnit

/**
 * Created by zuzik on 12/20/16.
 */
class RefreshTokenRequestObservableFactory(private val simulatedError: SimulatedError) {

    fun create(): (Token, Scheduler) -> Observable<Token> = { token, scheduler ->
        Observable
                .timer(4L, TimeUnit.SECONDS, scheduler)
                .flatMap {
                    when (simulatedError) {
                        SimulatedError.NO_INTERNET -> error(RuntimeException("no internet"))
                        SimulatedError.UNAUTHORIZED -> error(UnauthorizedException())
                        SimulatedError.NONE -> Observable.just(Token.validToken())
                    }
                }
                .subscribeOn(scheduler)
    }
}
