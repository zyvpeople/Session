package com.develop.zuzik.session

import rx.Observable
import rx.Observable.error
import rx.Observable.never
import rx.Scheduler
import rx.schedulers.Schedulers
import rx.subjects.BehaviorSubject
import java.util.concurrent.Executors

/**
 * Created by zuzik on 12/15/16.
 */
class Session<out Token>(validToken: Token,
                         private val unauthorizedStrategy: UnauthorizedStrategy,
                         private val refreshTokenRequestObservableFactory: (Token, Scheduler) -> Observable<Token>) {

    private val threadsSynchronizationScheduler = Schedulers.from(Executors.newSingleThreadExecutor())
    private val state: BehaviorSubject<SessionState> = BehaviorSubject.create(ValidTokenSessionState(validToken))

    fun <T> execute(requestObservableFactory: (Token) -> Observable<T>): Observable<T> {
        return state
                .filter { isNotTokenRefreshing(it) }
                .flatMap {
                    when (it) {
                        is ValidTokenSessionState<*> -> handleValidSessionState(it as ValidTokenSessionState<Token>, requestObservableFactory)
                        is UnauthorizedSessionState -> error(unauthorizedStrategy.createUnauthorizedError())
                        is InvalidTokenSessionState<*> -> handleInvalidSessionState<T>(it as InvalidTokenSessionState<Token>)
                        else -> error(NotImplementedError())
                    }
                }
                .take(1)
    }

    private fun isNotTokenRefreshing(it: SessionState) = it !is RefreshingTokenSessionState

    private fun <T> handleValidSessionState(state: ValidTokenSessionState<Token>, requestFactory: (Token) -> Observable<T>): Observable<T>? {
        val token = state.token
        return requestFactory(token)
                .observeOn(threadsSynchronizationScheduler)
                .onErrorResumeNext { error ->
                    this.state
                            .filter {
                                val stateIsNotChangedWhenThreadEnteredSynchronizedBlock = it is ValidTokenSessionState<*> && it.token == token
                                stateIsNotChangedWhenThreadEnteredSynchronizedBlock
                            }
                            .flatMap {
                                if (unauthorizedStrategy.isUnauthorizedError(error)) refreshTokenObservable<T>(token)
                                else error(error)
                            }
                }
    }

    private fun <T> handleInvalidSessionState(it: InvalidTokenSessionState<Token>): Observable<T> {
        val token = it.lastValidToken
        return state
                .observeOn(threadsSynchronizationScheduler)
                .filter {
                    val stateIsNotChangedWhenThreadEnteredSynchronizedBlock = it is InvalidTokenSessionState<*> && it.lastValidToken == token
                    stateIsNotChangedWhenThreadEnteredSynchronizedBlock
                }
                .flatMap { refreshTokenObservable<T>(token) }
    }

    private fun <T> refreshTokenObservable(lastValidToken: Token): Observable<T> {
        state.onNext(RefreshingTokenSessionState())
        return refreshTokenRequestObservableFactory(lastValidToken, threadsSynchronizationScheduler)
                .observeOn(threadsSynchronizationScheduler)
                .doOnNext { state.onNext(ValidTokenSessionState(it)) }
                .doOnError {
                    state.onNext(
                            if (unauthorizedStrategy.isUnauthorizedError(it)) UnauthorizedSessionState()
                            else InvalidTokenSessionState(lastValidToken))
                }
                .flatMap { never<T>() }
    }
}

private interface SessionState
private data class ValidTokenSessionState<out Token>(val token: Token) : SessionState
private data class InvalidTokenSessionState<out Token>(val lastValidToken: Token) : SessionState
private class RefreshingTokenSessionState : SessionState
private class UnauthorizedSessionState : SessionState
