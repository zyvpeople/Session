package com.develop.zuzik.sessionsample

import java.util.*

/**
 * Created by zuzik on 12/15/16.
 */
data class Token private constructor(val value: String) {
    companion object Factory {
        fun invalidToken() = Token("invalid")
        fun validToken() = Token(Random().nextInt().toString())
    }

    fun isValid() = value != "invalid"
}