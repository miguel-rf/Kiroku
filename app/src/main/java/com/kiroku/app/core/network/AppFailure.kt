package com.kiroku.app.core.network

enum class FailureKind {
    OFFLINE,
    TIMEOUT,
    AUTHENTICATION,
    PERMISSION,
    RATE_LIMITED,
    VALIDATION,
    NOT_FOUND,
    SERVER,
    SERIALIZATION,
    UNKNOWN,
}

class AppFailure(
    val kind: FailureKind,
    val diagnosticReason: String? = null,
    val retryAfterSeconds: Long? = null,
    cause: Throwable? = null,
) : Exception(diagnosticReason, cause)

internal fun invalidPayload(reason: String): Nothing = throw AppFailure(
    kind = FailureKind.SERIALIZATION,
    diagnosticReason = reason,
)
