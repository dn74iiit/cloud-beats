package com.cloudbeats.app.auth

/**
 * Represents the authentication state of the user.
 */
sealed class AuthState {
    /** Still checking if a cached account exists. */
    data object Loading : AuthState()

    /** User is not signed in. */
    data object SignedOut : AuthState()

    /** User is signed in with a valid account. */
    data class SignedIn(
        val displayName: String,
        val email: String,
        val accountId: String
    ) : AuthState()

    /** An error occurred during authentication. */
    data class Error(val message: String) : AuthState()
}
