package com.wingedsheep.gameserver.controller

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.shouldBe
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.server.LocalServerPort
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class FocusedMagicPrintingCorsTest(
    @param:LocalServerPort private val port: Int,
) : FunSpec({
    val http = HttpClient.newHttpClient()
    for (origin in listOf("https://seconddrawfocusedmagic.pages.dev", "http://127.0.0.1:8781")) {
        for (path in listOf(
            "/api/printings?names=Blood%20Crypt",
            "/api/printings?names=Commit%20%2F%2F%20Memory",
            "/api/cards/Bilbo%2C%20Thief%20in%20the%20Night/printings",
        )) {
            test("printing art is readable from $origin through $path") {
                val request = HttpRequest.newBuilder(URI.create("http://localhost:$port$path"))
                    .header("Origin", origin).GET().build()
                val response = http.send(request, HttpResponse.BodyHandlers.ofString())
                response.statusCode() shouldBe 200
                response.headers().firstValue("Access-Control-Allow-Origin").orElse("") shouldBe origin
                response.body() shouldContain "imageUri"
            }
        }
    }
})
