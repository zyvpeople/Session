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
                .filter { doesNotRefreshToken(it) }
                .observeOn(scheduler)
                .doOnNext { printThread("Session(flatMap)", id) }
                .flatMap {
                    when (it) {
                        is ValidTokenSessionState -> handleValidSessionShate(id, it, requestFactory)
                        is UnauthorizedSessionState -> error(UnauthorizedException())
                        is InvalidTokenSessionState -> handleInvalidSessionState<T>(id, it)
                        else -> error(NotImplementedError())
                    }
                }
                .take(1)
    }

    private fun doesNotRefreshToken(it: SessionState) = it !is RefreshingTokenSessionState

    private fun <T> handleValidSessionShate(id: Int, state: ValidTokenSessionState, requestFactory: (Token) -> Observable<T>): Observable<T>? {
        val token = state.token
        return requestFactory(token)
                .observeOn(scheduler)
                .doOnError { printThread("Session(onErrorResumeNext requestFactory)", id) }
                .onErrorResumeNext {
                    val exception = it
                    stateSubject
                            .filter { it is ValidTokenSessionState && it.token == token }
                            .flatMap {
                                when (exception) {
                                    is UnauthorizedException -> refreshTokenObservable<T>(id, token)
                                    else -> error(exception)
                                }
                            }
                }
    }

    private fun <T> handleInvalidSessionState(id: Int, it: InvalidTokenSessionState): Observable<T>? {
        val token = it.lastValidToken
        return stateSubject
                .filter { it is InvalidTokenSessionState && it.lastValidToken == token }
                .flatMap { refreshTokenObservable<T>(id, token) }
    }

    private fun <T> refreshTokenObservable(id: Int, lastValidToken: Token): Observable<T> {
        stateSubject.onNext(RefreshingTokenSessionState())
        return refreshTokenRequestFactory(lastValidToken, scheduler)
                .observeOn(scheduler)
                .doOnNext { printThread("Session(flatMap refreshTokenRequestFactory)", id) }
                .doOnNext { stateSubject.onNext(ValidTokenSessionState(it)) }
                .doOnError {
                    stateSubject.onNext(when (it) {
                        is UnauthorizedException -> UnauthorizedSessionState()
                        else -> InvalidTokenSessionState(lastValidToken)
                    })
                }
                .flatMap { never<T>() }
    }

    private fun printThread(tag: String, id: Int) {
        println("$tag: ${Thread.currentThread().id} id: $id")
    }
}

private interface SessionState
private data class ValidTokenSessionState(val token: Token) : SessionState
private data class InvalidTokenSessionState(val lastValidToken: Token) : SessionState
private class RefreshingTokenSessionState : SessionState
private class UnauthorizedSessionState : SessionState
