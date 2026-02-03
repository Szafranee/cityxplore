package app.cityxplore.social.domain.exception

/**
 * Exception thrown when a user attempts to send a friend invite to themselves.
 */
class CannotInviteSelfException : Exception("You cannot send a friend request to yourself")

/**
 * Exception thrown when a user is not found in the system.
 */
class UserNotFoundException(username: String) : Exception("User '$username' not found")
