package com.develop.zuzik.sessionsample

import com.develop.zuzik.session.UnauthorizedStrategy

/**
 * Created by zuzik on 12/20/16.
 */
class UnauthorizedStrategy : UnauthorizedStrategy {
    override fun createUnauthorizedError(): Throwable = UnauthorizedException()

    override fun isUnauthorizedError(error: Throwable): Boolean = error is UnauthorizedException
}