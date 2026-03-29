package eu.kanade.tachiyomi.extension.id.narasininja

import eu.kanade.tachiyomi.network.GET
import eu.kanade.tachiyomi.network.POST
import eu.kanade.tachiyomi.source.model.Filter
import eu.kanade.tachiyomi.source.model.FilterList
import eu.kanade.tachiyomi.source.model.MangasPage
import eu.kanade.tachiyomi.source.model.Page
import eu.kanade.tachiyomi.source.model.SChapter
import eu.kanade.tachiyomi.source.model.SManga
import eu.kanade.tachiyomi.source.online.HttpSource
import eu.kanade.tachiyomi.util.asJsoup
import keiyoushi.utils.parseAs
import keiyoushi.utils.tryParse
import okhttp3.FormBody
import okhttp3.Headers
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.Request
import okhttp3.Response
import java.text.SimpleDateFormat
import java.util.Locale

class NarasiNinja : HttpSource() {

    override val name = "NarasiNinja"
    override val baseUrl = "https://narasininja.net"
    override val lang = "id"
    override val supportsLatest = true

    // ── CSRF ──────────────────────────────────────────────────────────────────

    private var cachedCsrfToken: String? = null
    private var csrfTokenExpiry: Long = 0

    private fun getCsrfToken(): String {
        val now = System.currentTimeMillis()
        if (cachedCsrfToken != null && now < csrfTokenExpiry) {
            return cachedCsrfToken!!
        }

        synchronized(this) {
            if (cachedCsrfToken != null && now < csrfTokenExpiry) {
                return cachedCsrfToken!!
            }

            val url = baseUrl.toHttpUrl().newBuilder()
                .addPathSegment("komik")
                .build()
            val response = client.newCall(GET(url.toString(), headers)).execute()
            val token = response.use {
                it.asJsoup()
                    .selectFirst("meta[name=csrf-token]")
                    ?.attr("content")
            } ?: error("CSRF token not found")

            cachedCsrfToken = token
            csrfTokenExpiry = now + CSRF_TOKEN_VALIDITY_MS
            return token
        }
    }

    private fun filterHeaders(): Headers = headers.newBuilder()
        .add("X-CSRF-TOKEN", getCsrfToken())
        .add("X-Requested-With", "XMLHttpRequest")
        .add("Referer", "$baseUrl/komik")
        .build()

    // ── FILTER REQUEST / PARSE ────────────────────────────────────────────────

    private fun buildFilterRequest(page: Int, query: String, filters: FilterList): Request {
        var status = ""
        var type = ""
        var order = ""
        val genres = mutableListOf<String>()

        filters.forEach { filter ->
            when (filter) {
                is StatusFilter -> status = filter.selectedValue()
                is TypeFilter -> type = filter.selectedValue()
                is OrderFilter -> order = filter.selectedValue()
                is GenreFilter ->
                    filter.state
                        .filter { it.state }
                        .forEach { genres.add(it.value) }
                else -> {}
            }
        }

        val url = baseUrl.toHttpUrl().newBuilder()
            .addPathSegment("komik")
            .addPathSegment("filter")
            .addQueryParameter("page", page.toString())
            .build()

        val body = FormBody.Builder()
            .add("search", query)
            .add("status", status)
            .add("type", type)
            .add("order", order)
            .also { b -> genres.forEach { b.add("genre[]", it) } }
            .build()

        return POST(url.toString(), filterHeaders(), body)
    }

    private fun filterParse(response: Response): MangasPage {
        val result = response.parseAs<FilterResponse>()
        return MangasPage(
            result.data.map { it.toSManga(baseUrl) },
            result.meta.currentPage < result.meta.lastPage,
        )
    }

    // ── POPULAR ───────────────────────────────────────────────────────────────

    override fun popularMangaRequest(page: Int): Request = GET(baseUrl, headers)

    override fun popularMangaParse(response: Response): MangasPage {
        val doc = response.asJsoup()
        val mangas = doc.select(".listupd.popularslider .bs .bsx a").map { a ->
            SManga.create().apply {
                title = a.attr("title")
                setUrlWithoutDomain(a.attr("href"))
                thumbnail_url = a.selectFirst("img")?.attr("abs:src")
            }
        }
        return MangasPage(mangas, false)
    }

    // ── LATEST ────────────────────────────────────────────────────────────────

    override fun latestUpdatesRequest(page: Int) = buildFilterRequest(page, "", FilterList(OrderFilter().apply { state = 3 }))

    override fun latestUpdatesParse(response: Response) = filterParse(response)

    // ── SEARCH ────────────────────────────────────────────────────────────────

    override suspend fun getSearchManga(page: Int, query: String, filters: FilterList): MangasPage = kotlinx.coroutines.coroutineScope {
        val response = client.newCall(searchMangaRequest(page, query, filters))
            .execute()
        if (!response.isSuccessful) {
            response.close()
            throw IllegalStateException("HTTP error ${response.code}")
        }
        searchMangaParse(response)
    }

    override fun searchMangaRequest(page: Int, query: String, filters: FilterList) = buildFilterRequest(page, query, filters)

    override fun searchMangaParse(response: Response) = filterParse(response)

    // ── MANGA DETAILS ─────────────────────────────────────────────────────────

    override fun mangaDetailsRequest(manga: SManga): Request {
        val url = (baseUrl + manga.url).toHttpUrl().newBuilder().build()
        return GET(url.toString(), headers)
    }

    override fun mangaDetailsParse(response: Response): SManga {
        val doc = response.asJsoup()
        return SManga.create().apply {
            title = doc.selectFirst("h1.entry-title")?.text() ?: "Unknown"
            thumbnail_url = doc.selectFirst(".thumb img")?.attr("abs:src")
            description = doc.selectFirst(".entry-content.entry-content-single p")?.text()
            status = doc.selectFirst(".infotable tr:contains(Status) td:last-child")
                ?.text().toStatus()
            genre = doc.select(".seriestugenre a").joinToString { it.text() }
            author = doc.selectFirst(".infotable tr:contains(Author) td:last-child")
                ?.text().takeUnless { it.isNullOrBlank() || it == "-" }
            artist = doc.selectFirst(".infotable tr:contains(Artist) td:last-child")
                ?.text().takeUnless { it.isNullOrBlank() || it == "-" }
        }
    }

    // ── CHAPTER LIST ──────────────────────────────────────────────────────────

    override fun chapterListRequest(manga: SManga): Request {
        val url = (baseUrl + manga.url).toHttpUrl().newBuilder().build()
        return GET(url.toString(), headers)
    }

    private val chapterNumberRegex = Regex("""(\d+)(?:[._-](\d+))?""")

    private fun parseChapterNumber(raw: String): Float {
        val match = chapterNumberRegex.find(raw.trim()) ?: return -1f
        val major = match.groupValues[1].toIntOrNull() ?: return -1f
        val minor = match.groupValues[2].toIntOrNull()
        return if (minor != null) {
            "$major.$minor".toFloat()
        } else {
            major.toFloat()
        }
    }

    override fun chapterListParse(response: Response): List<SChapter> {
        val doc = response.asJsoup()
        return doc.select("#chapterlist li, .eplister li")
            .mapNotNull { li ->
                li.selectFirst("a")?.let { a ->
                    SChapter.create().apply {
                        setUrlWithoutDomain(a.attr("href"))
                        name = li.selectFirst(".chapternum")?.text() ?: a.text()
                        date_upload = li.selectFirst(".chapterdate")?.text()
                            ?.let { parseDate(it) } ?: 0L
                        chapter_number = parseChapterNumber(
                            li.attr("data-num").ifEmpty {
                                name.substringAfterLast(" ")
                            },
                        )
                    }
                }
            }
            .distinctBy { it.chapter_number }
            .sortedByDescending { it.chapter_number }
    }

    // ── PAGES ─────────────────────────────────────────────────────────────────

    override fun pageListRequest(chapter: SChapter): Request {
        val url = (baseUrl + chapter.url).toHttpUrl().newBuilder().build()
        return GET(url.toString(), headers)
    }

    override fun pageListParse(response: Response): List<Page> {
        val doc = response.asJsoup()
        return doc.select("#readerarea img.ts-main-image").mapIndexed { i, img ->
            Page(i, imageUrl = img.attr("abs:src"))
        }
    }

    override fun imageUrlParse(response: Response) = throw UnsupportedOperationException()

    // ── HELPERS ───────────────────────────────────────────────────────────────

    private val dateFormat = SimpleDateFormat("MMMM dd, yyyy", Locale.US)

    private fun parseDate(text: String): Long = dateFormat.tryParse(text)

    companion object {
        private const val CSRF_TOKEN_VALIDITY_MS = 3_600_000 // 1 hour
    }

    // ── FILTERS ───────────────────────────────────────────────────────────────

    override fun getFilterList() = FilterList(
        StatusFilter(),
        TypeFilter(),
        OrderFilter(),
        Filter.Separator(),
        GenreFilter(),
    )
}
