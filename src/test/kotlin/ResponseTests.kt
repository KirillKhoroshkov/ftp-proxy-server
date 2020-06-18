import org.junit.Assert.assertEquals
import org.junit.Test
import org.polykek.ftpproxyserver.response.MessageLine
import org.polykek.ftpproxyserver.response.Response.Handled.*
import java.nio.file.Path

class ResponseTests {

    @Test
    fun first() {
        val response = PathStatus(listOf(MessageLine("\"home/j\"\"\"\"/g\"")))
        val result = response.takePath()
        assertEquals(Path.of("home/j\"\"/g"), result)
    }

    @Test
    fun second() {
        val response = OpeningDataConnection(listOf(MessageLine("Opening BINARY mode data connection for bla.txt (256 bytes).")))
        val size = response.takeSize()
        assertEquals(256L, size)
    }
}