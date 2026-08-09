package com.example.data.model

enum class AppCategory(val displayName: String) {
    WORK("Work"),
    BROWSER("Browser"),
    GAMES("Games"),
    ENTERTAINMENT("Entertainment"),
    COMMUNICATION("Communication"),
    SYSTEM("System"),
    OTHER("Other");

    companion object {
        fun fromString(value: String): AppCategory {
            return entries.firstOrNull { 
                it.name.equals(value, ignoreCase = true) || it.displayName.equals(value, ignoreCase = true) 
            } ?: OTHER
        }
    }
}
