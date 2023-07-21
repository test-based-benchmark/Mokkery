package dev.mokkery.internal.answering

import dev.mokkery.MockMode
import dev.mokkery.answering.Answer
import dev.mokkery.answering.FunctionScope
import dev.mokkery.internal.CallContext
import dev.mokkery.internal.CallNotMockedException
import dev.mokkery.internal.ConcurrentTemplatingException
import dev.mokkery.internal.MokkeryInterceptor
import dev.mokkery.internal.matcher.CallMatcher
import dev.mokkery.internal.templating.CallTemplate
import dev.mokkery.internal.tracing.CallArg
import dev.mokkery.internal.tracing.CallTrace
import kotlinx.atomicfu.atomic
import kotlin.reflect.KClass

internal interface AnsweringInterceptor : MokkeryInterceptor {

    val answers: Map<CallTemplate, Answer<*>>

    fun setup(template: CallTemplate, answer: Answer<*>)

    fun reset()
}

internal fun AnsweringInterceptor(mockMode: MockMode, callMatcher: CallMatcher = CallMatcher()): AnsweringInterceptor {
    return AnsweringInterceptorImpl(mockMode, callMatcher)
}

private class AnsweringInterceptorImpl(
    private val mockMode: MockMode,
    private val callMatcher: CallMatcher,
) : AnsweringInterceptor {

    private var isSetup by atomic(false)
    private var _answers by atomic(linkedMapOf<CallTemplate, Answer<*>>())
    override val answers: Map<CallTemplate, Answer<*>> get() = _answers

    override fun setup(template: CallTemplate, answer: Answer<*>) {
        isSetup = true
        _answers += template to answer
        isSetup = false
    }

    override fun reset() {
        _answers = linkedMapOf()
    }

    override fun interceptCall(context: CallContext): Any? {
        if (isSetup) throw ConcurrentTemplatingException()
        return findAnswerFor(context).call(context.toFunctionScope())
    }

    override suspend fun interceptSuspendCall(context: CallContext): Any? {
        if (isSetup) throw ConcurrentTemplatingException()
        return findAnswerFor(context).callSuspend(context.toFunctionScope())
    }

    private fun findAnswerFor(context: CallContext): Answer<*> {
        val trace = CallTrace(receiver = context.self.id, name = context.name, args = context.args, orderStamp = 0)
        val answers = this._answers
        return answers
            .keys
            .reversed()
            .find { callMatcher.matches(trace, it) }
            ?.let { answers.getValue(it) }
            ?: handleMissingAnswer(trace, context.returnType)
    }

    private fun handleMissingAnswer(trace: CallTrace, returnType: KClass<*>): Answer<*> = when {
        mockMode == MockMode.autofill -> Answer.Autofill
        mockMode == MockMode.autoUnit && returnType == Unit::class -> Answer.Const(Unit)
        else -> throw CallNotMockedException(trace.toString())
    }

    private fun CallContext.toFunctionScope() = FunctionScope(returnType, args.map(CallArg::value), self)
}
