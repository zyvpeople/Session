package com.develop.zuzik.sessionsample

import com.develop.zuzik.session.Session
import rx.Observable.*
import rx.schedulers.Schedulers
import java.util.*
import java.util.concurrent.TimeUnit

/**
 * Created by zuzik on 12/15/16.
 */

val session = Session(
        Token.invalidToken(),
        UnauthorizedStrategy(),
        RefreshTokenRequestObservableFactory(SimulatedError.NONE).create())

fun main(args: Array<String>) {

    val tokenChangedObservable = session.token
    val authorizationFailedObservable = session.unauthorized

    tokenChangedObservable
            .doOnNext { println("save token $it to settings") }
            .subscribe()

    authorizationFailedObservable
            .doOnNext { println("clear settings") }
            .subscribe()

    performFakeRequest(1)
    performFakeRequest(2)
    performFakeRequest(3)

    readLine()
}

fun performFakeRequest(requestId: Int) {
    session
            .execute({ token ->
                timer(Random().nextInt(5).toLong(), TimeUnit.SECONDS)
                        .flatMap {
                            val shouldSimulateUnauthorizedError = !token.isValid()
                            if (shouldSimulateUnauthorizedError) {
                                error<Int>(UnauthorizedException())
                            } else {
                                just(it)
                            }
                        }
                        .subscribeOn(Schedulers.newThread())
            })
            .subscribe(
                    { println("onNext: requestId=$requestId") },
                    { println("onError: requestId=$requestId error=$it") },
                    { println("onCompleted: requestId=$requestId") })
}