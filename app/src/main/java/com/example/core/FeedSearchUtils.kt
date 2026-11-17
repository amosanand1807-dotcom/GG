package com.example.core

import com.example.models.CityPost

/**
 * Matches a [CityPost] against a user search query containing hashtags (`#tag`)
 * and/or general keywords. All space-separated tokens must match (AND semantics).
 */
fun matchesPostSearchQuery(post: CityPost, rawQuery: String): Boolean {
    val trimmed = rawQuery.trim()
    if (trimmed.isEmpty()) return true

    val tokens = trimmed.split(Regex("\\s+")).filter { it.isNotEmpty() }
    if (tokens.isEmpty()) return true

    return tokens.all { token ->
        if (token.startsWith("#")) {
            val tagTerm = token.removePrefix("#")
            if (tagTerm.isEmpty()) {
                post.tags.isNotEmpty() || post.caption.contains("#")
            } else {
                post.tags.any { it.contains(tagTerm, ignoreCase = true) } ||
                    post.caption.contains("#$tagTerm", ignoreCase = true)
            }
        } else {
            post.caption.contains(token, ignoreCase = true) ||
                post.tags.any { it.contains(token, ignoreCase = true) } ||
                post.authorName.contains(token, ignoreCase = true) ||
                post.authorHandle.contains(token, ignoreCase = true) ||
                post.neighborhood.contains(token, ignoreCase = true) ||
                post.communityName.contains(token, ignoreCase = true)
        }
    }
}

/**
 * Collects the most frequent non-blank hashtags across loaded city posts for quick filter chips.
 */
fun extractTrendingHashtags(posts: List<CityPost>, maxCount: Int = 6): List<String> {
    if (posts.isEmpty() || maxCount <= 0) return emptyList()
    val counts = linkedMapOf<String, Pair<String, Int>>()
    for (post in posts) {
        for (rawTag in post.tags) {
            val clean = rawTag.trim().removePrefix("#")
            if (clean.isEmpty()) continue
            val key = clean.lowercase()
            val existing = counts[key]
            if (existing == null) {
                counts[key] = clean to 1
            } else {
                counts[key] = existing.first to (existing.second + 1)
            }
        }
    }
    return counts.values
        .sortedByDescending { it.second }
        .take(maxCount)
        .map { it.first }
}
