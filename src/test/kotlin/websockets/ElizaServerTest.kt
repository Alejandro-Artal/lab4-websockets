package websockets

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment.*
import org.springframework.boot.web.server.LocalServerPort
import java.net.URI
import java.util.concurrent.CountDownLatch
import javax.websocket.*


@SpringBootTest(webEnvironment = RANDOM_PORT)
class ElizaServerTest {

    private lateinit var container: WebSocketContainer

    @LocalServerPort
    private var port: Int = 0

    @BeforeEach
    fun setup() {
        container = ContainerProvider.getWebSocketContainer()
    }

    @Test
    fun onOpen() {
        val latch = CountDownLatch(3)
        val list = mutableListOf<String>()

        val client = ElizaOnOpenMessageHandler(list, latch)
        container.connectToServer(client, URI("ws://localhost:$port/eliza"))
        latch.await()
        assertEquals(3, list.size)
        assertEquals("The doctor is in.", list[0])
    }

    @Test
    fun onChat() {
        val latch = CountDownLatch(4)
        val list = mutableListOf<String>()

        val client = ElizaOnOpenMessageHandlerToComplete(list, latch)
        container.connectToServer(client, URI("ws://localhost:$port/eliza"))
        latch.await()
        assertEquals(4, list.size) //check there have benn 4 messages in total
        val iAmResponses = listOf(
            "I am sorry to hear you are afraid.", "How long have you been afraid?",
            "Do you believe it is normal to be afraid?", "Do you enjoy being afraid?"
        )
        assert(iAmResponses.contains(list[3])) //check the response is valid
    }

}

@ClientEndpoint
class ElizaOnOpenMessageHandler(private val list: MutableList<String>, private val latch: CountDownLatch)  {
    @OnMessage
    fun onMessage(message: String) {
        list.add(message)
        latch.countDown()
    }
}

@ClientEndpoint
class ElizaOnOpenMessageHandlerToComplete(private val list: MutableList<String>, private val latch: CountDownLatch)  {

    @OnMessage
    fun onMessage(message: String, session: Session)  {
        list.add(message)
        latch.countDown()
        System.out.println("Message from the doctor: " + message)
        System.out.println("Latch: " + latch.count)
        //the first two messages are introductory from the doctor
        //so the third is for the pacient to tell the doctor what happens
        //then the fourth will be the response from the doctor
        if (latch.count == 1L) {
            session.basicRemote.sendText("i am afraid")
        }
    }
}
