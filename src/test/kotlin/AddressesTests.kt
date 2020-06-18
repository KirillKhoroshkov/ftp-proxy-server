import org.junit.Assert.assertEquals
import org.junit.Test
import org.polykek.ftpproxyserver.util.*
import java.net.InetSocketAddress

class AddressesTests {

    @Test
    fun first() {
        val socketAddress = InetSocketAddress("192.168.6.100", 15605)
        val result = convertSocketAddressToStringEnumeration(socketAddress)
        assertEquals("192,168,6,100,60,245", result)
    }

    @Test
    fun second() {
        val socketAddress = InetSocketAddress("192.168.6.100", 15605)
        val result = convertStringEnumerationToSocketAddress("192,168,6,100,60,245")
        assertEquals(socketAddress, result)
    }
}