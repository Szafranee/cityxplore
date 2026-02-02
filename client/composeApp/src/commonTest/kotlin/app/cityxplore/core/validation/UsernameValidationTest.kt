package app.cityxplore.core.validation

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Validates usernames according to application rules.
 *
 * Rules:
 * - Length: 3-30 characters
 * - Characters: Alphanumeric, underscores, hyphens
 * - No spaces
 * - Must start with a letter
 */
fun isValidUsername(username: String): Boolean {
    // Check length
    if (username.length !in 3..30) return false

    // Check characters (alphanumeric, underscore, hyphen)
    // Using simple regex: ^[a-zA-Z0-9_-]+$
    // Also must start with a letter: ^[a-zA-Z]
    val validChars = username.all { it.isLetterOrDigit() || it == '_' || it == '-' }
    if (!validChars) return false

    // Must start with a letter
    if (!username.first().isLetter()) return false

    return true
}

/**
 * Comprehensive tests for Username Validation logic.
 *
 * Ensures usernames meet security and display requirements:
 * - Length constraints
 * - Character set constraints
 * - Format constraints
 */
class UsernameValidationTest {

    @Test
    fun `valid usernames should pass validation`() {
        val validUsernames = listOf(
            "user123",
            "john_doe",
            "alice-wonder",
            "Bobby",
            "commander_shepard",
            "User-Name-123",
            "a_very_long_valid_username"
        )

        validUsernames.forEach { username ->
            assertTrue(
                isValidUsername(username),
                "Username '$username' should be valid"
            )
        }
    }

    @Test
    fun `usernames with invalid length should fail`() {
        val invalidLengths = listOf(
            "ab",           // Too short (2 chars)
            "",             // Empty
            "a",            // Too short (1 char)
            "a".repeat(31), // Too long (31 chars)
            "this_username_is_way_too_long_for_our_system" // Very long
        )

        invalidLengths.forEach { username ->
            assertFalse(
                isValidUsername(username),
                "Username length ${username.length} should be invalid"
            )
        }
    }

    @Test
    fun `usernames with invalid characters should fail`() {
        val invalidChars = listOf(
            "user name",    // Space
            "user@name",    // Special char @
            "user#name",    // Special char #
            "user\$name",   // Special char $
            "user.name",    // Dot (often restricted)
            "user/name",    // Slash
            "user\\name",   // Backslash
            "user'name",    // Quote
            "user\"name",   // Double quote
            "user\nname"    // Newline
        )

        invalidChars.forEach { username ->
            assertFalse(
                isValidUsername(username),
                "Username '$username' should be invalid due to characters"
            )
        }
    }

    @Test
    fun `usernames must start with a letter`() {
        val invalidStarts = listOf(
            "1user",        // Starts with a number
            "_user",        // Starts with underscore
            "-user",        // Starts with hyphen
            "99problems"    // Starts with a number
        )

        invalidStarts.forEach { username ->
            assertFalse(
                isValidUsername(username),
                "Username '$username' should be invalid (must start with letter)"
            )
        }
    }

    @Test
    fun `boundary testing for length`() {
        // Minimum valid length (3)
        assertTrue(isValidUsername("abc"), "Length 3 should be valid")

        // Maximum valid length (30)
        assertTrue(isValidUsername("a".repeat(30)), "Length 30 should be valid")

        // Just below minimum (2)
        assertFalse(isValidUsername("ab"), "Length 2 should be invalid")

        // Just above maximum (31)
        assertFalse(isValidUsername("a".repeat(31)), "Length 31 should be invalid")
    }
}
