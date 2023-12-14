package com.example.demo

import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.inputStream
import kotlin.io.path.outputStream
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.boot.web.reactive.function.client.WebClientCustomizer
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.io.buffer.DataBuffer
import org.springframework.core.io.buffer.DataBufferUtils
import org.springframework.core.io.buffer.DefaultDataBufferFactory
import org.springframework.stereotype.Component
import org.springframework.stereotype.Service
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.reactive.function.client.ExchangeFilterFunction
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.bodyToMono
import org.springframework.web.reactive.function.server.ServerResponse
import org.springframework.web.reactive.function.server.router
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

@SpringBootApplication class DemoApplication

@Configuration
class TestClientConfig {

  @Bean
  fun testWebClient(
      @Value("\${test.client.url}") appUrl: String,
      webClientBuilder: WebClient.Builder
  ): WebClient = webClientBuilder.baseUrl(appUrl).build()
}

@Component
class Customizer : WebClientCustomizer {
  private val bufferFactory = DefaultDataBufferFactory()
  private val log = LoggerFactory.getLogger(javaClass)

  override fun customize(webClientBuilder: WebClient.Builder) {
    webClientBuilder.filter(
        ExchangeFilterFunction.ofResponseProcessor {
          Mono.just(it.mutate().body(::writeToTmpFile).build())
        },
    )
  }

  fun writeToTmpFile(oldData: Flux<DataBuffer>): Flux<DataBuffer> =
      Mono.fromCallable { Files.createTempFile("sample", ".out") }
          .doOnNext { log.info("Writing response data to file {}", it) }
          .flatMap { writeBuffersToPath(oldData, it).thenReturn(it) }
          .doOnNext { log.info("Response data saved to file {}", it) }
          .flatMapMany { DataBufferUtils.readInputStream(it::inputStream, bufferFactory, 4096) }

  fun writeBuffersToPath(buffer: Flux<DataBuffer>, path: Path) =
      Mono.using(
          { path.outputStream() },
          { DataBufferUtils.write(buffer, it).then() },
          { it.close() },
      )
}

data class Post(
    val id: Int,
    val title: String,
    val author: String,
)

@Service
class TestClient(private val testWebClient: WebClient) {
  fun getPost(): Mono<Post> = testWebClient.get().uri("/posts/1").retrieve().bodyToMono<Post>()
}

@RestController
@RequestMapping("/test")
class Controller(private val testClient: TestClient) {

  @GetMapping fun request(): Mono<Post> = testClient.getPost()
}

@Configuration
class ReactiveRouter {
  @Bean
  fun reactiveRoute(testClient: TestClient) = router {
    GET("/test2") { testClient.getPost().flatMap { ServerResponse.ok().bodyValue(it) } }
  }
}

fun main(args: Array<String>) {
  runApplication<DemoApplication>(*args)
}
