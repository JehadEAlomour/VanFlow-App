package com.jehadalomour.flowvan.core.domain.usecase

import co.touchlab.kermit.Logger
import com.jehadalomour.flowvan.core.datastore.SessionStore
import com.jehadalomour.flowvan.core.network.api.AuthApi

/** What the server said, or that it said nothing. */
enum class RequirementAnswer {
    /** The server answered: this rep must have location on. */
    REQUIRED,

    /** The server answered: this rep is not required to. */
    NOT_REQUIRED,

    /** No answer — offline, a rejected token, anything. NOT the same as either. */
    UNKNOWN,
}

/**
 * Ask the server whether this rep is required to have location on.
 *
 * WHY THE THREE-WAY ANSWER. The requirement is cached in the session, and the
 * cache is what nearly bricked a handset: the lock screen replaces every other
 * screen, so nothing syncs while it is up, and a rep stayed locked out long
 * after the office had switched the requirement off. Answering with a plain
 * Boolean repeats the mistake one level down, because it cannot say "I do not
 * know" — and "I did not manage to ask" being indistinguishable from "yes" is
 * precisely what locks someone out of their own working day.
 *
 * The session is still written on a real answer, so everything else in the app
 * that reads it keeps working the way it did.
 */
class RefreshLocationRequirementUseCase(
    private val authApi: AuthApi,
    private val session: SessionStore,
) {
    private val log = Logger.withTag("LocationRequirement")

    suspend operator fun invoke(): RequirementAnswer = try {
        val me = authApi.me()
        // Absent key = a server that does not know about this at all, which must
        // not lock anybody out — the same rule login applies.
        val required = me.permissions["requireLocation"] == true
        session.requireLocation = required
        if (required) RequirementAnswer.REQUIRED else RequirementAnswer.NOT_REQUIRED
    } catch (e: Exception) {
        log.w("could not refresh the location requirement: ${e.message}")
        RequirementAnswer.UNKNOWN
    }
}
