package com.example.demo

import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.web.reactive.server.WebTestClient
import org.springframework.test.web.reactive.server.expectBody
import reactor.kotlin.test.test

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class DemoApplicationTests
@Autowired
constructor(
    private val testClient: TestClient,
    private val webTestClient: WebTestClient,
) {

  @Test
  fun whenUsingWebClientDirectlyThenItWorks() {
    testClient.getPost().test().expectNext(Post(1, "json-server", "typicode")).verifyComplete()
  }

  @Test
  fun whenUsingControllerThenItCrashes() {
    webTestClient
        .get()
        .uri("/test")
        .exchange()
        .expectStatus()
        .isOk
        .expectBody()
        .jsonPath("$.id")
        .isEqualTo(1)
        .jsonPath("$.title")
        .isEqualTo("json-server")
        .jsonPath("$.author")
        .isEqualTo("typicode")
  }

  @Test
  fun whenUsingFunctionalRouteThenItWorks() {
    webTestClient
        .get()
        .uri("/test2")
        .exchange()
        .expectStatus()
        .isOk
        .expectBody()
        .jsonPath("$.id")
        .isEqualTo(1)
        .jsonPath("$.title")
        .isEqualTo("json-server")
        .jsonPath("$.author")
        .isEqualTo("typicode")
  }
}
