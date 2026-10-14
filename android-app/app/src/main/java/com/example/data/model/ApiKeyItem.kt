package com.example.data.model

import java.util.UUID

data class ApiKeyItem(
    val id: String = UUID.randomUUID().toString(),
    val key: String,
    val label: String = "API Key",
    val isExhausted: Boolean = false,
    val lastUsedAt: Long = 0L,
    val exhaustedAt: Long = 0L
) {
    val maskedKey: String
        get() = if (key.length > 8) {
            "${key.take(6)}...${key.takeLast(4)}"
        } else {
            "••••••••"
        }
}
