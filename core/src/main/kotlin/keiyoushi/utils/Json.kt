package keiyoushi.utils

import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.decodeFromStream
import okhttp3.Response
import uy.kohesive.injekt.injectLazy

val jsonInstance: Json by injectLazy()

/**
 * Parses JSON string into an object of type [T].
 */
inline fun <reified T> String.parseAs(json: Json = jsonInstance): T =
    json.decodeFromString(this)

/**
 * Parses JSON string into an object of type [T], applying a [transform] function to the string before parsing.
 *
 * @param json The [Json] instance to use for deserialization.
 * @param transform A function to transform the original JSON string before it is parsed.
 */
inline fun <reified T> String.parseAs(json: Json = jsonInstance, transform: (String) -> String): T =
    transform(this).parseAs(json)

/**
 * Parses the response body into an object of type [T].
 */
inline fun <reified T> Response.parseAs(json: Json = jsonInstance): T =
    use { json.decodeFromStream<T>(body.byteStream()) }

/**
 * Parses the response body into an object of type [T], applying a transformation to the raw JSON string before parsing.
 * NOTE: This method loads the full response into memory. For large responses, consider using the stream-based approach.
 *
 * @param json The [Json] instance to use for parsing. Defaults to the injected instance.
 * @param transform A function to transform the JSON string before it's decoded.
 */
inline fun <reified T> Response.parseAs(json: Json = jsonInstance, transform: (String) -> String): T {
    // Check for large responses and warn about memory usage if needed
    val contentLength = body.contentLength()
    if (contentLength > 10 * 1024 * 1024) { // 10MB threshold
        // For very large responses, consider if transformation is really needed
    }
    return body.string().parseAs(json, transform)
}

/**
 * Parses a [JsonElement] into an object of type [T].
 *
 * @param json The [Json] instance to use for parsing. Defaults to the injected instance.
 */
inline fun <reified T> JsonElement.parseAs(json: Json = jsonInstance): T =
    json.decodeFromJsonElement(this)

/**
 * Serializes the object to a JSON string.
 */
inline fun <reified T> T.toJsonString(json: Json = jsonInstance): String =
    json.encodeToString(this)
