package keiyoushi.utils

import java.text.ParseException
import java.text.SimpleDateFormat

@Suppress("NOTHING_TO_INLINE")
inline fun SimpleDateFormat.tryParse(date: String?): Long {
    if (date == null) return 0L

    return try {
        val parsedDate = parse(date)
        parsedDate?.time ?: 0L
    } catch (e: ParseException) {
        0L
    }
}
