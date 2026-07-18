package com.wiva.android.data.remote.telemetry.mvp

import java.io.IOException
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

internal class FakeEpochClock(
    initialMs: Long = 1_000_000L,
) : EpochMillisClock {
    var nowMs: Long = initialMs

    override fun epochMillis(): Long = nowMs

    fun advanceSeconds(seconds: Long) {
        nowMs += seconds * 1000L
    }
}

class MachineJwtCacheTest {
    private lateinit var clock: FakeEpochClock
    private lateinit var cache: MachineJwtCache

    @Before
    fun setUp() {
        clock = FakeEpochClock()
        cache = MachineJwtCache(clock)
    }

    @Test
    fun `reuse cached token within ttl`() = runTest {
        // given
        var fetchCount = 0
        // when
        val first =
            cache.getAccessToken("WIVA-1", "sec") {
                fetchCount++
                Result.success(TokenResponseDto("jwt_one", "Bearer", 3600))
            }.getOrThrow()
        val second =
            cache.getAccessToken("WIVA-1", "sec") {
                fetchCount++
                Result.success(TokenResponseDto("jwt_two", "Bearer", 3600))
            }.getOrThrow()
        // then
        assertEquals("jwt_one", first)
        assertEquals("jwt_one", second)
        assertEquals(1, fetchCount)
    }

    @Test
    fun `refresh near expiry skew`() = runTest {
        // given
        var fetchCount = 0
        cache
            .getAccessToken("WIVA-1", "sec") {
                fetchCount++
                Result.success(TokenResponseDto("jwt_one", "Bearer", 3600))
            }.getOrThrow()
        clock.advanceSeconds(3600 - 60)
        // when
        val refreshed =
            cache.getAccessToken("WIVA-1", "sec") {
                fetchCount++
                Result.success(TokenResponseDto("jwt_two", "Bearer", 3600))
            }.getOrThrow()
        // then
        assertEquals("jwt_two", refreshed)
        assertEquals(2, fetchCount)
    }

    @Test
    fun `concurrent requests single flight`() = runTest {
        // given
        var fetchCount = 0
        // when
        val tokens =
            (1..5)
                .map {
                    async {
                        cache.getAccessToken("WIVA-1", "sec") {
                            fetchCount++
                            Result.success(TokenResponseDto("jwt_shared", "Bearer", 3600))
                        }.getOrThrow()
                    }
                }.awaitAll()
        // then
        assertTrue(tokens.all { it == "jwt_shared" })
        assertEquals(1, fetchCount)
    }

    @Test
    fun `401 invalidates cache`() = runTest {
        // given
        cache
            .getAccessToken("WIVA-1", "sec") {
                Result.success(TokenResponseDto("jwt_one", "Bearer", 3600))
            }.getOrThrow()
        clock.advanceSeconds(3600 - 60)
        cache
            .getAccessToken("WIVA-1", "sec") {
                Result.failure(TokenAuthException("HTTP 401"))
            }.getOrElse { assertTrue(it is TokenAuthException) }
        var fetchCount = 0
        // when
        val token =
            cache.getAccessToken("WIVA-1", "sec") {
                fetchCount++
                Result.success(TokenResponseDto("jwt_new", "Bearer", 3600))
            }.getOrThrow()
        // then
        assertEquals("jwt_new", token)
        assertEquals(1, fetchCount)
    }

    @Test
    fun `network failure keeps valid cached token`() = runTest {
        // given
        cache
            .getAccessToken("WIVA-1", "sec") {
                Result.success(TokenResponseDto("jwt_one", "Bearer", 3600))
            }.getOrThrow()
        clock.advanceSeconds(3600 - 60)
        // when
        val token =
            cache.getAccessToken("WIVA-1", "sec") {
                Result.failure(IOException("offline"))
            }.getOrThrow()
        // then
        assertEquals("jwt_one", token)
    }

    @Test
    fun `identity change invalidates cache`() = runTest {
        // given
        cache
            .getAccessToken("WIVA-1", "sec_a") {
                Result.success(TokenResponseDto("jwt_one", "Bearer", 3600))
            }.getOrThrow()
        var fetchCount = 0
        // when
        val token =
            cache.getAccessToken("WIVA-1", "sec_b") {
                fetchCount++
                Result.success(TokenResponseDto("jwt_two", "Bearer", 3600))
            }.getOrThrow()
        // then
        assertEquals("jwt_two", token)
        assertEquals(1, fetchCount)
    }

    @Test
    fun `refresh skew capped for short ttl`() {
        assertEquals(3, MachineJwtCache.refreshSkewSeconds(30))
        assertEquals(60, MachineJwtCache.refreshSkewSeconds(3600))
    }
}
