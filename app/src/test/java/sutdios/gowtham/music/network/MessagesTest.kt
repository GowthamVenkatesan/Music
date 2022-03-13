package sutdios.gowtham.music.network

import android.net.nsd.NsdServiceInfo
import android.util.Log
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.Mockito.*
import org.powermock.api.mockito.PowerMockito
import org.powermock.core.classloader.annotations.PrepareForTest
import java.io.OutputStream
import java.net.Socket
import org.powermock.modules.junit4.PowerMockRunner
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.net.InetAddress
import java.nio.ByteBuffer
import kotlin.random.Random

@RunWith(PowerMockRunner::class)
@PrepareForTest(Log::class)
class MessagesTest {

    @Mock
    lateinit var mockService: Service

    @Mock
    lateinit var mockSocket: Socket

    @Mock
    lateinit var mockOutputStream: OutputStream

    @Mock
    lateinit var mockNsdInfo: NsdServiceInfo

    @Mock
    lateinit var mockInetAdderss: InetAddress

    @Before
    fun beforeTest() {
        PowerMockito.mockStatic(Log::class.java)
    }

    @Test
    fun testPlay() {

        val writtenBytes = ByteArrayOutputStream()
        var redirectedInputStream: InputStream? = null

        `when`(mockService.socket).thenReturn(mockSocket)
        `when`(mockService.state).thenReturn(Service.State.CONNECTED)

        `when`(mockSocket.getOutputStream()).thenReturn(mockOutputStream)
        `when`(mockOutputStream.write(any(ByteArray::class.java))).thenAnswer { writtenBytes.write(ByteBuffer.wrap(it.getArgumentAt(0, ByteArray::class.java)).array()) }

        Control.prepare(mockService, "song1234", 1234212L)

        redirectedInputStream = ByteArrayInputStream(writtenBytes.toByteArray())
        `when`(mockSocket.getInputStream()).thenReturn(redirectedInputStream)

        val readMessage = Message.receive(mockService) as Control

        Assert.assertEquals(readMessage.action, Control.Action.PLAY)
        Assert.assertEquals(readMessage.additionalInfo, "song1234")
        Assert.assertEquals(readMessage.time, 1234212L)
    }

    @Test
    fun testPause() {

        val writtenBytes = ByteArrayOutputStream()
        var redirectedInputStream: InputStream? = null

        `when`(mockService.socket).thenReturn(mockSocket)
        `when`(mockService.state).thenReturn(Service.State.CONNECTED)

        `when`(mockSocket.getOutputStream()).thenReturn(mockOutputStream)
        `when`(mockOutputStream.write(any(ByteArray::class.java))).thenAnswer { writtenBytes.write(ByteBuffer.wrap(it.getArgumentAt(0, ByteArray::class.java)).array()) }

        Control.pause(mockService)

        redirectedInputStream = ByteArrayInputStream(writtenBytes.toByteArray())
        `when`(mockSocket.getInputStream()).thenReturn(redirectedInputStream)

        val readMessage = Message.receive(mockService) as Control

        Assert.assertEquals(readMessage.action, Control.Action.PAUSE)
        Assert.assertEquals(readMessage.additionalInfo, "")
        Assert.assertEquals(readMessage.time, -1L)
    }

    @Test
    fun testStop() {

        val writtenBytes = ByteArrayOutputStream()
        var redirectedInputStream: InputStream? = null

        `when`(mockService.socket).thenReturn(mockSocket)
        `when`(mockService.state).thenReturn(Service.State.CONNECTED)

        `when`(mockSocket.getOutputStream()).thenReturn(mockOutputStream)
        `when`(mockOutputStream.write(any(ByteArray::class.java))).thenAnswer { writtenBytes.write(ByteBuffer.wrap(it.getArgumentAt(0, ByteArray::class.java)).array()) }

        Control.stop(mockService)

        redirectedInputStream = ByteArrayInputStream(writtenBytes.toByteArray())
        `when`(mockSocket.getInputStream()).thenReturn(redirectedInputStream)

        val readMessage = Message.receive(mockService) as Control

        Assert.assertEquals(readMessage.action, Control.Action.STOP)
        Assert.assertEquals(readMessage.additionalInfo, "")
        Assert.assertEquals(readMessage.time, -1L)
    }

    @Test
    fun testReqShareSong() {
        // Assume it works
    }

    @Test
    fun testStreamSong() {

        val writtenBytes = ByteArrayOutputStream()
        var redirectedInputStream: InputStream? = null

        `when`(mockService.socket).thenReturn(mockSocket)
        `when`(mockService.state).thenReturn(Service.State.CONNECTED)

        `when`(mockSocket.getOutputStream()).thenReturn(mockOutputStream)
        `when`(mockOutputStream.write(any(ByteArray::class.java))).thenAnswer { writtenBytes.write(ByteBuffer.wrap(it.getArgumentAt(0, ByteArray::class.java)).array()) }

        val random = Random(System.currentTimeMillis())
        val dummyData = ByteArray(10 * 1000 * 1000) // 10 MB
        random.nextBytes(dummyData)

        Stream.streamSong("song101010", ByteArrayInputStream(dummyData), mockService)

        redirectedInputStream = ByteArrayInputStream(writtenBytes.toByteArray())
        `when`(mockSocket.getInputStream()).thenReturn(redirectedInputStream)

        val readMessage = Message.receive(mockService) as Stream

        Assert.assertEquals(readMessage.songName, "song101010")
        Assert.assertEquals(readMessage.audioBuffer, ByteBuffer.wrap(dummyData))
    }
}