package com.example.data.repository

import com.example.data.model.AppCategory

class CategoryResolver(
    private val customOverrides: Map<String, String> = emptyMap()
) {

    private val browserPackages = setOf(
        "com.android.chrome",
        "org.mozilla.firefox",
        "com.sec.android.app.sbrowser",
        "com.opera.browser",
        "com.brave.browser",
        "com.microsoft.emmx",
        "com.duckduckgo.mobile.android"
    )

    private val communicationPackages = setOf(
        "com.whatsapp",
        "com.facebook.orca",
        "com.google.android.talk",
        "com.slack",
        "com.discord",
        "org.telegram.messenger",
        "com.google.android.apps.messaging",
        "com.tencent.mm",
        "com.viber.voip",
        "com.microsoft.teams",
        "com.skype.raider",
        "com.android.dialer",
        "com.google.android.dialer"
    )

    private val entertainmentPackages = setOf(
        "com.google.android.youtube",
        "com.netflix.mediaclient",
        "com.spotify.music",
        "com.amazon.avod.thirdpartyclient",
        "tv.twitch.android.app",
        "com.hbo.hbonow",
        "com.disney.disneyplus",
        "com.zhiliaoapp.musically", // TikTok
        "com.instagram.android"
    )

    private val gamePackages = setOf(
        "com.epicgames.fortnite",
        "com.mojang.minecraftpe",
        "com.roblox.client",
        "com.pubg.krmobile",
        "com.king.candycrushsaga",
        "com.supercell.clashofclans",
        "com.nianticlabs.pokemongo"
    )

    private val workPackages = setOf(
        "com.microsoft.office.word",
        "com.microsoft.office.excel",
        "com.google.android.apps.docs",
        "com.google.android.apps.sheets",
        "com.google.android.apps.slides",
        "com.github.mobile",
        "com.google.android.gm",
        "com.microsoft.office.outlook",
        "com.notion.id",
        "com.todoist"
    )

    private val systemPackages = setOf(
        "com.android.settings",
        "com.android.systemui",
        "com.google.android.packageinstaller",
        "com.android.vending",
        "com.google.android.permissioncontroller"
    )

    fun resolveCategory(packageName: String): String {
        // 1. Manual Override
        customOverrides[packageName]?.let { override ->
            if (override.isNotBlank()) return override
        }

        // 2. Predefined Package Maps
        val pkgLower = packageName.lowercase()

        if (browserPackages.contains(pkgLower) || pkgLower.contains("browser") || pkgLower.contains("chrome")) {
            return AppCategory.BROWSER.displayName
        }
        if (communicationPackages.contains(pkgLower) || pkgLower.contains("chat") || pkgLower.contains("message") || pkgLower.contains("whatsapp")) {
            return AppCategory.COMMUNICATION.displayName
        }
        if (entertainmentPackages.contains(pkgLower) || pkgLower.contains("youtube") || pkgLower.contains("video") || pkgLower.contains("music") || pkgLower.contains("stream")) {
            return AppCategory.ENTERTAINMENT.displayName
        }
        if (gamePackages.contains(pkgLower) || pkgLower.contains("game") || pkgLower.contains("arcade")) {
            return AppCategory.GAMES.displayName
        }
        if (workPackages.contains(pkgLower) || pkgLower.contains("office") || pkgLower.contains("docs") || pkgLower.contains("mail")) {
            return AppCategory.WORK.displayName
        }
        if (systemPackages.contains(pkgLower) || pkgLower.startsWith("com.android.") || pkgLower.startsWith("com.google.android.providers.")) {
            return AppCategory.SYSTEM.displayName
        }

        return AppCategory.OTHER.displayName
    }

    companion object {
        val DEFAULT_IGNORE_PACKAGES = setOf(
            "com.android.systemui",
            "com.google.android.apps.nexuslauncher",
            "com.sec.android.app.launcher",
            "com.miui.home",
            "com.huawei.android.launcher",
            "com.aistudio.lyfstackagent.mobile",
            "com.example"
        )

        fun parseIgnoreList(rawMultilineText: String): Set<String> {
            val lines = rawMultilineText.lines()
                .map { it.trim() }
                .filter { it.isNotBlank() && !it.startsWith("#") }
                .toMutableSet()
            lines.addAll(DEFAULT_IGNORE_PACKAGES)
            return lines
        }
    }
}
