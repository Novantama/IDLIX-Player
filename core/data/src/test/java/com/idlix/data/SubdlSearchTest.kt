package com.idlix.data

import com.idlix.data.model.SubdlQuery
import com.idlix.data.repository.SubdlRepository
import com.idlix.data.util.SubdlTitleCleaner
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SubdlSearchTest {

    @Test
    fun testTitleCleanerRemovesYearAndTags() {
        val r1 = SubdlTitleCleaner.clean("Avatar: The Way of Water (2022)")
        assertEquals("Avatar: The Way of Water", r1.title)
        assertEquals(2022, r1.year)

        val r2 = SubdlTitleCleaner.clean("Loki Season 2 (2023) Sub Indo")
        assertEquals("Loki", r2.title)
        assertEquals(2023, r2.year)

        val r3 = SubdlTitleCleaner.clean("Breaking Bad S01E02 [1080p]")
        assertEquals("Breaking Bad", r3.title)
        assertNull(r3.year)

        val r4 = SubdlTitleCleaner.clean("Interstellar 2014")
        assertEquals("Interstellar", r4.title)
        assertEquals(2014, r4.year)

        val r5 = SubdlTitleCleaner.clean("Game of Thrones - Episode 5")
        assertEquals("Game of Thrones", r5.title)
        assertNull(r5.year)
    }

    @Test
    fun testBuildSearchUrlParameters() {
        val q1 = SubdlQuery(filmName = "Avatar", year = 2009, type = "movie")
        val url1 = SubdlRepository.buildSearchUrl(q1, "test_api_key")
        assertTrue(url1.contains("api_key=test_api_key"))
        assertTrue(url1.contains("film_name=Avatar"))
        assertTrue(url1.contains("year=2009"))
        assertTrue(url1.contains("type=movie"))
        assertTrue(url1.contains("languages=ID,EN"))

        val q2 = SubdlQuery(sdId = 12345L, type = "tv", season = 2, episode = 4)
        val url2 = SubdlRepository.buildSearchUrl(q2, "my_key")
        assertTrue(url2.contains("sd_id=12345"))
        assertTrue(url2.contains("type=tv"))
        assertTrue(url2.contains("season_number=2"))
        assertTrue(url2.contains("episode_number=4"))
    }

    @Test
    fun testCleanDownloadUrlStripsApiKeyToAvoidRateLimit() {
        val sub1 = com.idlix.data.model.SubdlSubtitle(
            url = "/subtitle/3799668-8724419.zip?api_key=subdl__VuGBV3ExlH8tW68l4wVEIsk6Mfnaxq1_FiONFN8Yg0",
            language = "ID"
        )
        assertEquals("https://dl.subdl.com/subtitle/3799668-8724419.zip", sub1.cleanDownloadUrl)
        assertEquals("https://dl.subdl.com/subtitle/3799668-8724419.zip", sub1.fullDownloadUrl)

        val sub2 = com.idlix.data.model.SubdlSubtitle(
            url = "https://dl.subdl.com/subtitle/12345.zip?foo=bar",
            language = "EN"
        )
        assertEquals("https://dl.subdl.com/subtitle/12345.zip", sub2.cleanDownloadUrl)
    }
}

