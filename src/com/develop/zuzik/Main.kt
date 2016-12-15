package com.develop.zuzik

import rx.Observable
import rx.Observable.error
import rx.Observable.just
import rx.schedulers.Schedulers
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

/**
 * Created by zuzik on 12/15/16.
 */

val session = Session(Token("0"), { just(Token("1")) })
val mainThreadScheduler = Schedulers.from(Executors.newSingleThreadExecutor())

fun main(args: Array<String>) {


    performRequest(4)
    performRequest(3)
    performRequest(2)
    performRequest(1)
//    performRequest(0)

    readLine()
}

fun performRequest(id: Int) {
    session
            .execute(id, {
                val token = it
                just(id)
                        .delay(id.toLong(), TimeUnit.SECONDS)
                        .observeOn(Schedulers.newThread())
                        .flatMap {
                            printThread("Main (flatMap)", id)
                            if (token.value == "0") {
                                error<Int>(UnauthorizedException())
                            } else {
                                just(it)
                            }
                        }
            })
            .observeOn(mainThreadScheduler)
            .subscribe(
                    {
                        println("onNext: id=$id")
                    },
                    {
                        println("onNext: id=$id value=$it")
                    },
                    {
                        println("onCompleted: id=$id")
                    })
}

private fun printThread(tag: String, id: Int) {
    println("$tag: ${Thread.currentThread().id} id: $id")
}