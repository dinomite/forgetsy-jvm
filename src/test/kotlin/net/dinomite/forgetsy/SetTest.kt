package net.dinomite.forgetsy

import org.junit.Assert.assertEquals
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test
import redis.clients.jedis.JedisPool
import redis.embedded.RedisServer
import java.time.Duration
import java.time.Instant

class SetTest {
    var jedisPool: JedisPool

    lateinit var lifetime: Duration
    lateinit var set: Set

    val name = "foobar"
    val binName = "foo_bin"

    init {
        val port = 16379

        val redisServer: RedisServer = RedisServer(port)
        redisServer.start()

        jedisPool = JedisPool("localhost", port)
    }

    @Before
    fun setup() {
        jedisPool.resource.use { it.flushAll() }

        lifetime = 7.days()
        set = Set(jedisPool, name, lifetime)
    }

    @Test
    fun create() {
        assertEquals("Stores mean lifetime in special key when created", lifetime, set.fetchLifetime())
        assertWithin(Instant.now(), set.fetchLastDecayedDate())
    }

    @Test
    fun create_withStartTime() {
        val start = 21.daysAgo()
        val set = Set(jedisPool, name, Duration.ofSeconds(300), start)

        assertWithin(start, set.fetchLastDecayedDate())
    }

    @Test
    fun create_reifiesExisting() {
        val sameSet = Set(jedisPool, name)

        val lifetime = set.fetchLifetime()
        val decayDate = set.fetchLastDecayedDate()
        assertEquals(lifetime, sameSet.fetchLifetime())
        assertEquals(decayDate, sameSet.fetchLastDecayedDate())
    }

    @Test
    fun create_failsForNonexistant() {
        try {
            Set(jedisPool, "does-not-exist")
            fail()
        } catch (e: IllegalStateException) {
            assertEquals("Set doesn't exist (pass lifetime to create it)", e.message)
        }

    }


    @Test
    fun increment() {
        set.increment(binName)
        jedisPool.resource.use { assertEquals("Increments counter correctly", 1.0, it.zscore(name, binName), .01) }
    }

    @Test
    fun incrementBatch() {
        set.increment(binName, 5.0)
        jedisPool.resource.use { assertEquals("Increments in batches", 5.0, it.zscore(name, binName), .01) }
    }


    @Test
    fun fetch_byBinName() {
        set.increment(binName, 2.0)
        assertEquals(2.0, set.fetch(binName, decay = false)!!, .01)
    }

    @Test
    fun fetch_TopN() {
        val otherBin = "bar_bin"
        set.increment(binName, 2.0)
        set.increment(otherBin, 1.0)

        assertEquals(mapOf(binName to 2.0, otherBin to 1.0), set.fetch(2, false))
    }

    @Test
    fun fetch_All() {
        val otherBin = "bar_bin"
        set.increment(binName, 2.0)
        set.increment(otherBin, 1.0)

        assertEquals(mapOf(binName to 2.0, otherBin to 1.0), set.fetch(decay = false))
    }


    @Test
    fun decay() {
        val start = 2.daysAgo()
        val now = Instant.now()
        val delta = now.epochSecond - start.epochSecond
        val lifetime = 7.days()
        val rate = 1 / lifetime.toDouble()

        val fooName = "foo_bin"
        val fooValue = 2.0
        val barName = "bar_bin"
        val barValue = 10.0

        val set = Set(jedisPool, "decay_test", lifetime, start)
        set.increment(fooName, fooValue)
        set.increment(barName, barValue)

        val decayedFoo = fooValue * Math.exp(- rate * delta)
        val decayedBar = barValue * Math.exp(- rate * delta)

        set.decayData()
        assertEquals(decayedFoo, set.fetch(fooName)!!, .01)
        assertEquals(decayedBar, set.fetch(barName)!!, .01)
    }

    @Test
    fun scrub() {
        val start = 365.daysAgo()
        val lifetime = 7.days()
        val set = Set(jedisPool, "decay_test", lifetime, start)

        set.increment(binName)

        assertEquals(0, set.fetch().values.size)
    }
}