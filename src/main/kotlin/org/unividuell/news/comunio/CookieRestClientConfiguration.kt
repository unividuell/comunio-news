package org.unividuell.news.comunio

import io.github.resilience4j.ratelimiter.RateLimiter
import io.github.resilience4j.ratelimiter.RateLimiterConfig
import org.apache.hc.client5.http.cookie.BasicCookieStore
import org.apache.hc.client5.http.impl.classic.HttpClients
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.MediaType
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory
import org.springframework.http.converter.json.JacksonJsonHttpMessageConverter
import org.springframework.web.client.RestClient
import org.zalando.logbook.Logbook
import org.zalando.logbook.spring.LogbookClientHttpRequestInterceptor
import java.time.Duration

@Configuration
class CookieRestClientConfiguration {

    private val rateLimiter = RateLimiter
        .of(
            "comunio-stats-api",
            RateLimiterConfig.custom()
                // max 5 requests during 5 seconds
                .limitForPeriod(5)
                .limitRefreshPeriod(Duration.ofSeconds(5))
                // parks thread for up to 10 seconds before throwing a RequestNotPermitted exception
                .timeoutDuration(Duration.ofSeconds(10))
                .build()
        )

    private val httpClient = HttpClients.custom()
        // cookie handling
        .setDefaultCookieStore(BasicCookieStore())
        .build()

    @Bean
    fun restClient(comunioConfig: ComunioConfig, logbook: Logbook) = RestClient.builder()
        .requestFactory(HttpComponentsClientHttpRequestFactory(httpClient))
        .baseUrl(comunioConfig.stats.baseUrl)
        .defaultHeader("User-Agent", comunioConfig.stats.userAgent)
        .defaultHeader("Accept-Language", "en-US,en;q=0.9")
        // be careful: logbook does not see headers added by the underlying client5 client (like req-header `Cookie`)
        .requestInterceptor(LogbookClientHttpRequestInterceptor(logbook))
        .requestInterceptor { request, body, execution ->
            RateLimiter.waitForPermission(rateLimiter)
            execution.execute(request, body)
        }

    // comunio lies about the content-type (`text/html` but it is `application/json`)!
    @Bean
    fun htmlJsonConverter() = JacksonJsonHttpMessageConverter().apply {
        supportedMediaTypes = listOf(MediaType.APPLICATION_JSON, MediaType.TEXT_HTML)
    }
}