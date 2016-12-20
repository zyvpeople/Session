package com.develop.zuzik.sessionsample

import rx.Observable
import rx.Scheduler
import java.util.concurrent.TimeUnit

/**
 * Created by zuzik on 12/20/16.
 */
class RefreshTokenRequestObservableFactory {

    fun create(): (Token, Scheduler) -> Observable<Token> = { token, scheduler ->
        Observable.just(Object())
                .delay(4L, TimeUnit.SECONDS, scheduler)
                .observeOn(scheduler)
//                    .flatMap { error<Token>(RuntimeException("no internet")) }
//                    .flatMap { error<Token>(UnauthorizedException()) }
                .flatMap { Observable.just(Token("1")) }
    }
}
