package keiyoushi.utils

import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull

class CollectionsTest {

    @Test
    fun firstInstance_returnsFirstMatchingElement() {
        val list = listOf("string", 42, true, 3.14)

        val result = list.firstInstance<Int>()

        assertEquals(42, result)
    }

    @Test
    fun firstInstance_throwsWhenNotFound() {
        val list = listOf("string", true, 3.14)

        assertFailsWith<NoSuchElementException> {
            list.firstInstance<Int>()
        }
    }

    @Test
    fun firstInstanceOrNull_returnsFirstMatchingElement() {
        val list = listOf("string", 42, true, 3.14)

        val result = list.firstInstanceOrNull<Int>()

        assertEquals(42, result)
    }

    @Test
    fun firstInstanceOrNull_returnsNullWhenNotFound() {
        val list = listOf("string", true, 3.14)

        val result = list.firstInstanceOrNull<Int>()

        assertNull(result)
    }

    @Test
    fun firstInstanceOrNull_returnsNullForEmptyList() {
        val list = emptyList<String>()

        val result = list.firstInstanceOrNull<String>()

        assertNull(result)
    }

    @Test
    fun firstInstance_withCustomClass() {
        data class Person(val name: String, val age: Int)

        val list = listOf(
            Person("Alice", 30),
            "string",
            Person("Bob", 25),
        )

        val result = list.firstInstance<Person>()

        assertEquals("Alice", result.name)
        assertEquals(30, result.age)
    }
}
