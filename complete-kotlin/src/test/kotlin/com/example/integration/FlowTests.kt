package com.example.integration

import com.rometools.rome.feed.synd.SyndEntryImpl
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.integration.endpoint.SourcePollingChannelAdapter
import org.springframework.integration.support.MessageBuilder
import org.springframework.messaging.MessageChannel
import java.io.File

@SpringBootTest(properties = [
    "auto.startup=false",  // we don't want to start the real feed
    "feed.file.name=Test"  // use a different file
])
class FlowTests(
    @Autowired private val newsAdapter: SourcePollingChannelAdapter,
    @Autowired private val news: MessageChannel
) {

    @Test
    fun test() {
        assertThat(newsAdapter.isRunning).isFalse()
        val syndEntry = SyndEntryImpl().apply {
            title = "Test Title"
            link = "http://characters/frodo"
        }
        val out = File("/tmp/si/Test")
        out.delete()
        assertThat(out.exists()).isFalse()
        news.send(MessageBuilder.withPayload(syndEntry).build())
        assertThat(out.exists()).isTrue()
        assertThat(out.bufferedReader().use { it.readLine() }).isEqualTo("Test Title @ http://characters/frodo")
        out.delete()
    }

}
