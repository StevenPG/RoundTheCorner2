package com.stevenpg.roundthecorner2.core.network.nominatim

import kotlinx.coroutines.runBlocking
import mockwebserver3.MockResponse
import mockwebserver3.MockWebServer
import okhttp3.OkHttpClient
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.IOException
import java.util.Locale

/** Exercises the real HTTP + JSON path against a local mock server. */
class NominatimClientTest {

    private val server = MockWebServer()
    private lateinit var client: NominatimClient

    @Before
    fun setUp() {
        server.start()
        client = NominatimClient(OkHttpClient(), server.url("/").toString())
    }

    @After
    fun tearDown() = server.close()

    @Test
    fun `builds the documented query and parses results`() = runBlocking {
        server.enqueue(MockResponse.Builder().body(SAMPLE).build())

        val results = client.search("1600 Pennsylvania", limit = 5, locale = Locale.GERMANY)

        val request = server.takeRequest()
        val url = request.url
        assertEquals("/search", url.encodedPath)
        assertEquals("1600 Pennsylvania", url.queryParameter("q"))
        assertEquals("jsonv2", url.queryParameter("format"))
        assertEquals("1", url.queryParameter("addressdetails"))
        assertEquals("5", url.queryParameter("limit"))
        assertEquals("de-DE", request.headers["Accept-Language"])

        val first = results.single()
        assertEquals("47.5755192", first.lat)
        assertEquals("Pennsylvania Avenue", first.address?.road)
        assertEquals("Bremerton", first.address?.city)
    }

    @Test(expected = IOException::class)
    fun `non-2xx responses throw`(): Unit = runBlocking {
        server.enqueue(MockResponse.Builder().code(503).build())
        client.search("x")
    }

    @Test
    fun `consecutive calls are spaced at least one second apart`() = runBlocking {
        repeat(2) { server.enqueue(MockResponse.Builder().body("[]").build()) }
        client.search("a")
        val start = System.nanoTime()
        client.search("b")
        val elapsedMs = (System.nanoTime() - start) / 1_000_000
        assertTrue("Second call came after only $elapsedMs ms", elapsedMs >= 900)
    }

    private companion object {
        val SAMPLE = """
            [{"place_id":1,"lat":"47.5755192","lon":"-122.6419508","name":"",
              "display_name":"1600, Pennsylvania Avenue, Bremerton, Washington, United States",
              "address":{"house_number":"1600","road":"Pennsylvania Avenue","city":"Bremerton",
                         "state":"Washington","country":"United States","country_code":"us"},
              "boundingbox":["0","0","0","0"]}]
        """.trimIndent()
    }
}
