package com.develop.zuzik.session

/**
 * Created by zuzik on 12/20/16.
 */
interface UnauthorizedStrategy {
    fun createUnauthorizedError(): Throwable
    fun isUnauthorizedError(error: Throwable): Boolean
}