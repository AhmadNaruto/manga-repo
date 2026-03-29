package keiyoushi.utils

import org.junit.Test
import java.text.SimpleDateFormat
import java.util.Locale
import kotlin.test.assertEquals

class DateTest {

    private val isoFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US)
    private val simpleFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)
    private val dateTimeFormat = SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.US)

    @Test
    fun tryParse_parsesValidISODate() {
        val date = "2024-03-15T10:30:00.000Z"

        val result = isoFormat.tryParse(date)

        assertEquals(1710498600000L, result)
    }

    @Test
    fun tryParse_parsesValidSimpleDate() {
        val date = "2024-01-01"

        val result = simpleFormat.tryParse(date)

        assertEquals(1704067200000L, result)
    }

    @Test
    fun tryParse_parsesValidDateTime() {
        val date = "25/12/2023 15:45:30"

        val result = dateTimeFormat.tryParse(date)

        assertEquals(1703519130000L, result)
    }

    @Test
    fun tryParse_returnsZeroForNullInput() {
        val result = isoFormat.tryParse(null)

        assertEquals(0L, result)
    }

    @Test
    fun tryParse_returnsZeroForEmptyString() {
        val result = isoFormat.tryParse("")

        assertEquals(0L, result)
    }

    @Test
    fun tryParse_returnsZeroForInvalidFormat() {
        val date = "not-a-date"

        val result = isoFormat.tryParse(date)

        assertEquals(0L, result)
    }

    @Test
    fun tryParse_returnsZeroForPartiallyInvalidDate() {
        val format = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        format.isLenient = false
        val date = "2024-13-45" // Invalid month and day

        val result = format.tryParse(date)

        assertEquals(0L, result)
    }

    @Test
    fun tryParse_handlesDifferentTimeZones() {
        val utcFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        utcFormat.timeZone = java.util.TimeZone.getTimeZone("UTC")

        val date = "2024-06-15"
        val result = utcFormat.tryParse(date)

        assertEquals(1718409600000L, result)
    }
}
