package eu.kanade.tachiyomi.multisrc.monochrome

import eu.kanade.tachiyomi.network.GET
import eu.kanade.tachiyomi.source.model.FilterList
import eu.kanade.tachiyomi.source.model.MangasPage
import eu.kanade.tachiyomi.source.model.Page
import eu.kanade.tachiyomi.source.model.SChapter
import eu.kanade.tachiyomi.source.model.SManga
import eu.kanade.tachiyomi.source.online.HttpSource
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import okhttp3.Headers
import okhttp3.Response

open class MonochromeCMS(
    override val name: String,
    override val baseUrl: String,
    override val lang: String,
) : HttpSource() {
    override val supportsLatest = false

    protected open val apiUrl by lazy {
        baseUrl.replaceFirst("://", "://api.")
    }

    private val json = Json { ignoreUnknownKeys = true; coerceInputValues = true }

    override fun headersBuilder() = Headers.Builder().set("Referer", "$baseUrl/")

    override fun fetchPopularManga(page: Int) = fetchSearchManga(page, "", FilterList())

    override fun searchMangaRequest(page: Int, query: String, filters: FilterList) = GET("$apiUrl/manga?limit=10&offset=${10 * (page - 1)}&title=$query", headers)

    override fun searchMangaParse(response: Response) = response.decode<Results>().let {
        MangasPage(it.map(::mangaFromAPI), it.hasNext)
    }

    override suspend fun getSearchManga(
        page: Int,
        query: String,
        filters: FilterList,
    ): MangasPage {
        if (!query.startsWith(UUID_QUERY)) {
            return super.getSearchManga(page, query, filters)
        }
        val req = GET("$apiUrl/manga/${query.substringAfter(UUID_QUERY)}")
        val response = client.newCall(req).execute()
        if (!response.isSuccessful) {
            response.close()
            throw Exception("HTTP error ${response.code}")
        }
        return MangasPage(listOf(mangaFromAPI(response.decode())), false)
    }

    override suspend fun getMangaDetails(manga: SManga) = manga.apply { initialized = true }

    override fun chapterListRequest(manga: SManga) = GET("$apiUrl/manga/${manga.url}/chapters", headers)

    override suspend fun getChapterList(manga: SManga): List<SChapter> {
        val response = client.newCall(chapterListRequest(manga)).execute()
        if (!response.isSuccessful) {
            response.close()
            throw Exception("HTTP error ${response.code}")
        }
        return response.decode<List<Chapter>>().map { ch ->
            SChapter.create().apply {
                name = ch.title
                url = manga.url + ch.parts
                chapter_number = ch.number
                date_upload = ch.timestamp
                scanlator = ch.scanGroup
            }
        }
    }

    override suspend fun getPageList(chapter: SChapter): List<Page> {
        val (uuid, version, length) = chapter.url.split('|')
        val pages = IntRange(1, length.toInt()).map {
            Page(it, "", "$apiUrl/media/$uuid/$it.jpg?version=$version")
        }
        return pages
    }

    override fun getMangaUrl(manga: SManga) = "$baseUrl/manga/${manga.url}"

    override fun getChapterUrl(chapter: SChapter) = "$baseUrl/chapters/${chapter.url.subSequence(37, 73)}"

    private fun mangaFromAPI(manga: Manga) = SManga.create().apply {
        url = manga.id
        title = manga.title
        author = manga.author
        artist = manga.artist
        description = manga.description
        thumbnail_url = apiUrl + manga.cover
        status = when (manga.status) {
            "ongoing", "hiatus" -> SManga.ONGOING
            "completed", "cancelled" -> SManga.COMPLETED
            else -> SManga.UNKNOWN
        }
    }

    private inline fun <reified T> Response.decode() = json.decodeFromString<T>(body.string())

    override fun popularMangaRequest(page: Int) = throw UnsupportedOperationException()

    override fun latestUpdatesRequest(page: Int) = throw UnsupportedOperationException()

    override fun mangaDetailsRequest(manga: SManga) = throw UnsupportedOperationException()

    override fun popularMangaParse(response: Response) = throw UnsupportedOperationException()

    override fun latestUpdatesParse(response: Response) = throw UnsupportedOperationException()

    override fun mangaDetailsParse(response: Response) = throw UnsupportedOperationException()

    override fun chapterListParse(response: Response) = throw UnsupportedOperationException()

    override fun pageListRequest(chapter: SChapter) = throw UnsupportedOperationException()

    override fun pageListParse(response: Response) = throw UnsupportedOperationException()

    override fun imageUrlParse(response: Response) = throw UnsupportedOperationException()
}
