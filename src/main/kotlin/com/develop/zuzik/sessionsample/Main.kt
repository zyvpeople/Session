package com.develop.zuzik.sessionsample

import com.develop.zuzik.session.Session
import rx.Observable.error
import rx.Observable.just
import rx.schedulers.Schedulers
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

/**
 * Created by zuzik on 12/15/16.
 */

val sessionSingleton = Session(
        Token("0"),
        UnauthorizedStrategy(),
        RefreshTokenRequestObservableFactory().create())
val mainThreadScheduler = Schedulers.from(Executors.newSingleThreadExecutor())

fun main(args: Array<String>) {

    sessionSingleton
            .token
            .doOnNext { println("save token $it to settings") }
            .subscribe()

    sessionSingleton
            .unauthorized
            .doOnNext { println("clear settings") }
            .subscribe()

    performRequest(10, 3L)
    performRequest(9)
    performRequest(8)
    performRequest(7)
    performRequest(6)
    performRequest(5)

    readLine()
}

fun performRequest(id: Int, delay: Long = 0L) {
    just(Object())
            .observeOn(Schedulers.newThread())
            .flatMap {
                sessionSingleton
                        .execute({
                            val token = it
                            just(id)
                                    .observeOn(Schedulers.newThread())
                                    .delay(delay, TimeUnit.SECONDS)
                                    .flatMap {
                                        if (token.value == "0") {
                                            error<Int>(UnauthorizedException())
                                        } else {
                                            just(it)
                                        }
                                    }
                        })
            }
            .observeOn(mainThreadScheduler)
            .subscribe(
                    {
                        println("onNext: id=$id")
                    },
                    {
                        println("onError: id=$id value=$it")
                    },
                    {
                        println("onCompleted: id=$id")
                    })
}

private fun printThread(tag: String, id: Int) {
    println("$tag: ${Thread.currentThread().id} id: $id")
}