package com.develop.zuzik

import rx.Observable
import rx.Observable.*
import rx.Scheduler
import rx.schedulers.Schedulers
import rx.subjects.BehaviorSubject
import java.util.concurrent.Executors

/**
 * Created by zuzik on 12/15/16.
 */
class Session(defaultToken: Token, private val refreshTokenRequestFactory: (Token, Scheduler) -> Observable<Token>) {

    private val scheduler = Schedulers.from(Executors.newSingleThreadExecutor())
    private val stateSubject: BehaviorSubject<SessionState> = BehaviorSubject.create(ValidTokenSessionState(defaultToken))

    fun <T> execute(id: Int, requestFactory: (Token) -> Observable<T>): Observable<T> {
        return stateSubject
                .filter { it !is RefreshingTokenSessionState }
                .observeOn(scheduler)
                .flatMap {
                    printThread("Session(flatMap)", id)
                    when (it) {
                        is ValidTokenSessionState -> {
                            val token = it.token
                            requestFactory(it.token)
                                    .observeOn(scheduler)
                                    .onErrorResumeNext {
                                        val exception = it
                                        printThread("Session(onErrorResumeNext requestFactory)", id)
                                        just(Object())
                                                .flatMap { stateSubject }
                                                .filter { it is ValidTokenSessionState && it.token == token }
                                                .flatMap {
                                                    if (exception is UnauthorizedException) {
                                                        stateSubject.onNext(RefreshingTokenSessionState())
                                                        refreshTokenRequestFactory(token, scheduler)
                                                                .observeOn(scheduler)
                                                                //todo handle error
                                                                .flatMap {
                                                                    printThread("Session(flatMap refreshTokenRequestFactory)", id)
                                                                    stateSubject.onNext(ValidTokenSessionState(it))
                                                                    never<T>()
                                                                }
                                                    } else {
                                                        Observable.error(exception)
                                                    }
                                                }
                                    }
                        }
                    //return correct error (InvalidTokenSessionState -> some error, UnauthorizedSessionState -> Unauthorized)
                        else -> error(NotImplementedError())
                    }
                }
                .take(1)
    }

    private fun printThread(tag: String, id: Int) {
        println("$tag: ${Thread.currentThread().id} id: $id")
    }
}

interface SessionState
data class ValidTokenSessionState(val token: Token) : SessionState
class InvalidTokenSessionState : SessionState
class RefreshingTokenSessionState : SessionState
class UnauthorizedSessionState : SessionState
