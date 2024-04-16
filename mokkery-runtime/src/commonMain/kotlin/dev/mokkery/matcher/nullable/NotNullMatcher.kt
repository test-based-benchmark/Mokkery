package dev.mokkery.matcher.nullable

import dev.mokkery.annotations.DelicateMokkeryApi
import dev.mokkery.internal.MissingMatchersForComposite
import dev.mokkery.matcher.ArgMatcher
import dev.mokkery.matcher.ArgMatchersScope
import dev.mokkery.matcher.any
import dev.mokkery.matcher.capture.propagateCapture
import dev.mokkery.matcher.matches

/**
 * Matches an argument that is not null and matches [matcher].
 */
@Suppress("UNUSED_PARAMETER")
public inline fun <reified T : Any> ArgMatchersScope.notNull(matcher: T = any()): T? = matches(NotNullMatcher())

/**
 * Matches an argument that is not null and matches [matcher].
 */
@DelicateMokkeryApi
public class NotNullMatcher<T>(public val matcher: ArgMatcher<T & Any>? = null) : ArgMatcher.Composite<T> {

    override fun matches(arg: T): Boolean = arg?.let(matcher!!::matches) ?: false

    override fun compose(matcher: ArgMatcher<T>): ArgMatcher.Composite<T> = NotNullMatcher(matcher = matcher)

    override fun isFilled(): Boolean = matcher != null

    override fun assertFilled() {
        if (matcher == null) {
            throw MissingMatchersForComposite("notNull", 1, listOfNotNull(matcher))
        }
    }

    override fun capture(value: T) {
        if (value != null) {
            listOfNotNull(matcher).propagateCapture(value)
        }
    }

    override fun toString(): String = "notNull($matcher)"

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || this::class != other::class) return false
        other as NotNullMatcher<*>
        return matcher == other.matcher
    }

    override fun hashCode(): Int = matcher?.hashCode() ?: 0
}
