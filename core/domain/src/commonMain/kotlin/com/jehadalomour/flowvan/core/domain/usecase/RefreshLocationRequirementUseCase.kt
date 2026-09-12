package com.jehadalomour.flowvan.core.domain.usecase

import co.touchlab.kermit.Logger
import com.jehadalomour.flowvan.core.datastore.SessionStore
import com.jehadalomour.flowvan.core.network.api.AuthApi

/**
 * Ask the server whether this rep is still required to have location on.
 *
 * THE DEADLOCK THIS EXISTS TO BREAK. The requirement is cached in the session
 * and refreshed as part of the catalogue sync — which is fine until the app is
 * locked, because the lock replaces every screen and nothing syncs any more. So
 * an office that turned the requirement OFF could not reach the handset: the rep
 * stared at a lock screen whose own cause had already been removed, and the only
 * ways out were signing out or clearing the app's data.
 *
 * The lock screen calls this so the one thing it still does is ask whether it
 * should still be there.
 *
 * A FAILED CALL CHANGES NOTHING. The requirement is the office's instruction and
 * the last one received stands: unlocking because the server could not be
 * reached would make aeroplane mode the way around the rule, and locking on a
 * failure would strand a rep whose van has no signal. Silence is not an answer
 * in either direction.
 */
class RefreshLocationRequirementUseCase(
    private val authApi: AuthApi,
    private val session: SessionStore,
) {
    private val log = Logger.withTag("LocationRequirement")

    /** Returns the requirement in force after asking; unchanged if unreachable. */
    suspend operator fun invoke(): Boolean {
        try {
            val me = authApi.me()
            // Absent key = an older server that does not know about this at all,
            // which must not lock anybody out — same rule as login.
            session.requireLocation = me.permissions["requireLocation"] == true
        } catch (e: Exception) {
            log.w("could not refresh the location requirement: ${e.message}")
        }
        return session.requireLocation
    }
}
