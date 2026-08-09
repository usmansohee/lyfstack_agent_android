package com.example

import com.example.data.model.AppCategory
import com.example.data.repository.CategoryResolver
import org.junit.Assert.assertEquals
import org.junit.Test

class CategoryResolverTest {

    @Test
    fun testBrowserCategoryResolution() {
        val resolver = CategoryResolver()
        assertEquals(AppCategory.BROWSER.displayName, resolver.resolveCategory("com.android.chrome"))
        assertEquals(AppCategory.BROWSER.displayName, resolver.resolveCategory("org.mozilla.firefox"))
    }

    @Test
    fun testCommunicationCategoryResolution() {
        val resolver = CategoryResolver()
        assertEquals(AppCategory.COMMUNICATION.displayName, resolver.resolveCategory("com.whatsapp"))
        assertEquals(AppCategory.COMMUNICATION.displayName, resolver.resolveCategory("com.slack"))
    }

    @Test
    fun testEntertainmentCategoryResolution() {
        val resolver = CategoryResolver()
        assertEquals(AppCategory.ENTERTAINMENT.displayName, resolver.resolveCategory("com.google.android.youtube"))
        assertEquals(AppCategory.ENTERTAINMENT.displayName, resolver.resolveCategory("com.spotify.music"))
    }

    @Test
    fun testGamesCategoryResolution() {
        val resolver = CategoryResolver()
        assertEquals(AppCategory.GAMES.displayName, resolver.resolveCategory("com.roblox.client"))
        assertEquals(AppCategory.GAMES.displayName, resolver.resolveCategory("com.epicgames.fortnite"))
    }

    @Test
    fun testWorkCategoryResolution() {
        val resolver = CategoryResolver()
        assertEquals(AppCategory.WORK.displayName, resolver.resolveCategory("com.github.mobile"))
        assertEquals(AppCategory.WORK.displayName, resolver.resolveCategory("com.google.android.apps.docs"))
    }

    @Test
    fun testSystemCategoryResolution() {
        val resolver = CategoryResolver()
        assertEquals(AppCategory.SYSTEM.displayName, resolver.resolveCategory("com.android.settings"))
    }

    @Test
    fun testManualOverridePriority() {
        val customMap = mapOf("com.android.chrome" to "Work")
        val resolver = CategoryResolver(customMap)
        assertEquals("Work", resolver.resolveCategory("com.android.chrome"))
    }

    @Test
    fun testIgnoreListParsing() {
        val raw = "com.custom.launcher\n# Comment line\ncom.other.app"
        val parsed = CategoryResolver.parseIgnoreList(raw)
        assert(parsed.contains("com.custom.launcher"))
        assert(parsed.contains("com.other.app"))
        assert(parsed.contains("com.android.systemui"))
    }
}
