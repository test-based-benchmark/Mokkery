package dev.mokkery.coroutines.internal.answering

import dev.mokkery.answering.FunctionScope
import dev.mokkery.coroutines.answering.Awaitable
import kotlinx.coroutines.awaitCancellation

internal data object AwaitCancellation : Awaitable<Nothing> {

    override suspend fun await(scope: FunctionScope): Nothing = awaitCancellation()

    override fun description(): String = "cancellation"
}
