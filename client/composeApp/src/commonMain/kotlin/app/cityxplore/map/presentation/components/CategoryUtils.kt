package app.cityxplore.map.presentation.components

import app.cityxplore.map.domain.PoiCategory

/**
 * Utility functions for POI categories.
 *
 * This file centralises all category-related helper functions to ensure consistency
 * across the app and avoid code duplication.
 *
 * @see getCategoryColor for category color definitions
 * @see getCategoryIcon for category icon definitions
 */

/**
 * Returns a user-friendly display name for the given [PoiCategory].
 *
 * These names are used in category badges, chips, and other UI elements.
 *
 * @param category The POI category.
 * @return A human-readable display name.
 */
fun getCategoryDisplayName(category: PoiCategory): String {
    return when (category) {
        PoiCategory.HISTORICAL -> "Historical"
        PoiCategory.CULTURAL -> "Cultural"
        PoiCategory.NATURE -> "Nature"
        PoiCategory.FOOD -> "Food & Dining"
        PoiCategory.SPORTS -> "Sports"
        PoiCategory.ENTERTAINMENT -> "Entertainment"
        PoiCategory.CUSTOM -> "Custom"
        PoiCategory.OTHER -> "Other"
        PoiCategory.UNKNOWN -> "Unknown"
    }
}

/**
 * Safely parses a string to [PoiCategory], returning [PoiCategory.OTHER] if parsing fails.
 *
 * This function handles:
 * - Case-insensitive parsing (e.g. "historical", "HISTORICAL", "Historical")
 * - Null or blank input strings
 * - Invalid category names
 *
 * @param categoryString The string representation of a category (nullable).
 * @return The parsed [PoiCategory] or [PoiCategory.OTHER] if parsing fails.
 */
fun safeParsePoiCategory(categoryString: String?): PoiCategory {
    if (categoryString.isNullOrBlank()) {
        return PoiCategory.OTHER
    }
    return try {
        PoiCategory.valueOf(categoryString.uppercase())
    } catch (_: IllegalArgumentException) {
        PoiCategory.OTHER
    }
}

/**
 * Formats a category string for display with proper capitalisation.
 *
 * Example: "HISTORICAL" -> "Historical", "food" -> "Food"
 *
 * @param categoryString The raw category string.
 * @return Properly capitalised category name.
 */
fun formatCategoryName(categoryString: String?): String {
    if (categoryString.isNullOrBlank()) {
        return "Other"
    }

    return categoryString.lowercase().replaceFirstChar { it.uppercase() }
}
