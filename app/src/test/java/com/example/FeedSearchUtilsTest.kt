package com.example

import com.example.core.extractTrendingHashtags
import com.example.core.matchesPostSearchQuery
import com.example.models.CityPost
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class FeedSearchUtilsTest {

    private val postChai = CityPost(
        id = "1",
        authorName = "Aarav Mehta",
        authorHandle = "aarav_blr",
        neighborhood = "Indiranagar",
        caption = "Morning filter coffee and masala #chai walk near 100ft road!",
        tags = listOf("chai", "Indiranagar", "morning"),
        communityName = "Indiranagar Walkers"
    )

    private val postCycle = CityPost(
        id = "2",
        authorName = "Diya Nair",
        authorHandle = "diya_rides",
        neighborhood = "Koramangala",
        caption = "Sunday cycling group starting from 4th Block #cycling",
        tags = listOf("cycling", "fitness"),
        communityName = "BLR Cyclists"
    )

    @Test
    fun blankQueryMatchesAllPosts() {
        assertTrue(matchesPostSearchQuery(postChai, ""))
        assertTrue(matchesPostSearchQuery(postChai, "   "))
    }

    @Test
    fun hashtagQueryMatchesTagsArrayAndInlineCaptionHashtags() {
        assertTrue(matchesPostSearchQuery(postChai, "#chai"))
        assertTrue(matchesPostSearchQuery(postChai, "#CHAI"))
        assertTrue(matchesPostSearchQuery(postCycle, "#cycling"))
        assertFalse(matchesPostSearchQuery(postCycle, "#chai"))
    }

    @Test
    fun keywordQueryMatchesCaptionAuthorAreaAndCommunity() {
        assertTrue(matchesPostSearchQuery(postChai, "filter coffee"))
        assertTrue(matchesPostSearchQuery(postChai, "Aarav"))
        assertTrue(matchesPostSearchQuery(postChai, "Indiranagar"))
        assertTrue(matchesPostSearchQuery(postCycle, "BLR Cyclists"))
        assertFalse(matchesPostSearchQuery(postCycle, "coffee"))
    }

    @Test
    fun combinedHashtagAndKeywordTokensRequireAllTokens() {
        assertTrue(matchesPostSearchQuery(postChai, "#chai Indiranagar"))
        assertFalse(matchesPostSearchQuery(postChai, "#chai Koramangala"))
    }

    @Test
    fun trendingHashtagsRankByFrequencyCaseInsensitively() {
        val posts = listOf(
            postChai,
            postCycle,
            CityPost(id = "3", tags = listOf("CHAI", "fitness")),
            CityPost(id = "4", tags = listOf("chai"))
        )
        val trending = extractTrendingHashtags(posts, maxCount = 3)
        assertEquals(listOf("chai", "fitness", "Indiranagar"), trending)
    }
}
