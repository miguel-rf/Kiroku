package com.kiroku.app.core.common

import com.kiroku.app.core.network.AppFailure
import com.kiroku.app.core.network.FailureKind

enum class UiError {
    OFFLINE,
    TIMEOUT,
    AUTHENTICATION,
    PERMISSION,
    RATE_LIMITED,
    VALIDATION,
    NOT_FOUND,
    SERVER,
    DATA,
    UNKNOWN,
}

fun Throwable.toUiError(): UiError {
    val failure =
        generateSequence(this as Throwable?) { it.cause }
            .filterIsInstance<AppFailure>()
            .firstOrNull()
    return when (failure?.kind) {
        FailureKind.OFFLINE -> UiError.OFFLINE
        FailureKind.TIMEOUT -> UiError.TIMEOUT
        FailureKind.AUTHENTICATION -> UiError.AUTHENTICATION
        FailureKind.PERMISSION -> UiError.PERMISSION
        FailureKind.RATE_LIMITED -> UiError.RATE_LIMITED
        FailureKind.VALIDATION -> UiError.VALIDATION
        FailureKind.NOT_FOUND -> UiError.NOT_FOUND
        FailureKind.SERVER -> UiError.SERVER
        FailureKind.SERIALIZATION -> UiError.DATA
        FailureKind.UNKNOWN, null -> UiError.UNKNOWN
    }
}
