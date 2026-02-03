package app.cityxplore.map.presentation.components

import app.cityxplore.map.domain.PoiCategory
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Unit tests for [CategoryUtils].
 *
 * Checks:
 * - Parsing categories from strings (safe parsing)
 * - Formatting display names
 * - Handling invalid inputs
 */
class CategoryUtilsTest {

    // ==================== safeParsePoiCategory Tests ====================

    @Test
    fun `safeParsePoiCategory should parse valid categories correctly`() {
        assertEquals(PoiCategory.HISTORICAL, safeParsePoiCategory("HISTORICAL"))
        assertEquals(PoiCategory.NATURE, safeParsePoiCategory("NATURE"))
        assertEquals(PoiCategory.FOOD, safeParsePoiCategory("FOOD"))
    }

    @Test
    fun `safeParsePoiCategory should be case-insensitive`() {
        assertEquals(PoiCategory.HISTORICAL, safeParsePoiCategory("historical"))
        assertEquals(PoiCategory.CULTURAL, safeParsePoiCategory("Cultural"))
        assertEquals(PoiCategory.SPORTS, safeParsePoiCategory("sPoRtS"))
    }

    @Test
    fun `safeParsePoiCategory should handle invalid inputs gracefully`() {
        // Unknown category
        assertEquals(PoiCategory.OTHER, safeParsePoiCategory("NON_EXISTENT_CATEGORY"))

        // Null or empty
        assertEquals(PoiCategory.OTHER, safeParsePoiCategory(null))
        assertEquals(PoiCategory.OTHER, safeParsePoiCategory(""))
        assertEquals(PoiCategory.OTHER, safeParsePoiCategory("   "))

        // Symbols
        assertEquals(PoiCategory.OTHER, safeParsePoiCategory("$$$"))
    }

    // ==================== getCategoryDisplayName Tests ====================

    @Test
    fun `getCategoryDisplayName should return human-readable names`() {
        assertEquals("Historical", getCategoryDisplayName(PoiCategory.HISTORICAL))
        assertEquals("Food & Dining", getCategoryDisplayName(PoiCategory.FOOD))
        assertEquals("Nature", getCategoryDisplayName(PoiCategory.NATURE))
        assertEquals("Unknown", getCategoryDisplayName(PoiCategory.UNKNOWN))
    }

    // ==================== formatCategoryName Tests ====================

    @Test
    fun `formatCategoryName should capitalise first letter`() {
        assertEquals("Historical", formatCategoryName("HISTORICAL"))
        assertEquals("Food", formatCategoryName("food"))
        assertEquals("Sports", formatCategoryName("Sports"))
    }

    @Test
    fun `formatCategoryName should handle edge cases`() {
        assertEquals("Other", formatCategoryName(null))
        assertEquals("Other", formatCategoryName(""))
        assertEquals("A", formatCategoryName("a"))
    }
}
