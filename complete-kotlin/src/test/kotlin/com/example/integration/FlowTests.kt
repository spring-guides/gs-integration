package com.example.integration

import com.rometools.rome.feed.synd.SyndEntryImpl
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.integration.endpoint.SourcePollingChannelAdapter
import org.springframework.integration.support.MessageBuilder
import org.springframework.messaging.MessageChannel
import java.io.BufferedReader
import java.io.File
import java.io.FileReader

@SpringBootTest(properties = ["auto.startup=false",   // We don't want to start the real feed
				  "feed.file.name=Test"])              // Use a different file
class FlowTests {

	@Autowired
	private lateinit var newsAdapter: SourcePollingChannelAdapter

	@Autowired
	private lateinit var news: MessageChannel

	@Test
	fun test() {
		assertThat(newsAdapter.isRunning).isFalse()
		val syndEntry = SyndEntryImpl()
		syndEntry.title = "Test Title"
		syndEntry.link = "http://characters/frodo"
		val out = File("/tmp/si/Test")
		out.delete()
		assertThat(out.exists()).isFalse()
		news.send(MessageBuilder.withPayload(syndEntry).build())
		assertThat(out.exists()).isTrue()
		BufferedReader(FileReader(out)).use { br ->
			val line = br.readLine()
			assertThat(line).isEqualTo("Test Title @ http://characters/frodo")
		}
		out.delete()
	}
}
