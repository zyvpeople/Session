package com.develop.zuzik

import com.develop.zuzik.session.Session
import com.develop.zuzik.session.UnauthorizedStrategy
import rx.Observable.error
import rx.Observable.just
import rx.schedulers.Schedulers
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

/**
 * Created by zuzik on 12/15/16.
 */

val session = Session(
        Token("0"),
        object : UnauthorizedStrategy {
            override fun createUnauthorizedError(): Throwable = UnauthorizedException()

            override fun isUnauthorizedError(error: Throwable): Boolean = error is UnauthorizedException
        },
        { token, scheduler ->
            just(Object())
                    .delay(4L, TimeUnit.SECONDS, scheduler)
                    .observeOn(scheduler)
//                    .flatMap { error<Token>(RuntimeException("no internet")) }
//                    .flatMap { error<Token>(UnauthorizedException()) }
                    .flatMap { just(Token("1")) }
        })
val mainThreadScheduler = Schedulers.from(Executors.newSingleThreadExecutor())

fun main(args: Array<String>) {

    session
            .token
            .doOnNext { println("settings token $it") }
            .subscribe()

    session
            .unauthorized
            .doOnNext { println("settings unauthorized") }
            .subscribe()

    performRequest(10, 3L)
    performRequest(9)
    performRequest(8)
    performRequest(7)
    performRequest(6)
    performRequest(5)
//    performRequest(4)
//    performRequest(3)
//    performRequest(2)
//    performRequest(1)
//    performRequest(0)

    readLine()
}

fun performRequest(id: Int, delay: Long = 0L) {
    just(Object())
            .observeOn(Schedulers.newThread())
            .flatMap {
                session
                        .execute({
                            val token = it
                            println("Main (token) $token")
                            just(id)
                                    .observeOn(Schedulers.newThread())
                                    .delay(delay, TimeUnit.SECONDS)
                                    .flatMap {
                                        printThread("Main (flatMap)", id)
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