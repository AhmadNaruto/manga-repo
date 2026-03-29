package eu.kanade.tachiyomi.extension.id.roseveil

import eu.kanade.tachiyomi.network.GET
import eu.kanade.tachiyomi.network.interceptor.rateLimit
import eu.kanade.tachiyomi.source.model.FilterList
import eu.kanade.tachiyomi.source.model.MangasPage
import eu.kanade.tachiyomi.source.model.Page
import eu.kanade.tachiyomi.source.model.SChapter
import eu.kanade.tachiyomi.source.model.SManga
import eu.kanade.tachiyomi.source.online.HttpSource
import eu.kanade.tachiyomi.util.asJsoup
import keiyoushi.utils.tryParse
import okhttp3.Headers
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.Request
import okhttp3.Response
import org.jsoup.nodes.Element
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.text.SimpleDateFormat
import java.util.Locale

class Roseveil : HttpSource() {

    // URL path change (/manga/ -> /comic/)
    override val versionId = 2

    override val name = "Roseveil"

    override val baseUrl = "https://roseveil.org"

    override val lang = "id"

    override val supportsLatest = true

    override val client = network.cloudflareClient.newBuilder()
        .rateLimit(2)
        .build()

    override fun headersBuilder() = Headers.Builder()
        .add("Referer", "$baseUrl/")

    // ============================== Popular & Latest ==============================
    override fun popularMangaRequest(page: Int): Request {
        val url = "$baseUrl/allcomic".toHttpUrl().newBuilder().apply {
            addQueryParameter("q", "")
            addQueryParameter("status", "")
            addQueryParameter("genre", "")
            addQueryParameter("comic_type", "")
            addQueryParameter("color_format", "")
            addQueryParameter("reading_format", "")
            addQueryParameter("author", "")
            addQueryParameter("artist", "")
            addQueryParameter("publisher", "")
            addQueryParameter("sort", "views")
            addQueryParameter("order", "desc")
            addQueryParameter("page", page.toString())
        }.build()

        return GET(url, headers)
    }

    override fun popularMangaParse(response: Response): MangasPage = parseMangaPage(response)

    override fun latestUpdatesRequest(page: Int): Request {
        val url = "$baseUrl/allcomic".toHttpUrl().newBuilder().apply {
            addQueryParameter("q", "")
            addQueryParameter("status", "")
            addQueryParameter("genre", "")
            addQueryParameter("comic_type", "")
            addQueryParameter("color_format", "")
            addQueryParameter("reading_format", "")
            addQueryParameter("author", "")
            addQueryParameter("artist", "")
            addQueryParameter("publisher", "")
            addQueryParameter("sort", "new")
            addQueryParameter("order", "desc")
            addQueryParameter("page", page.toString())
        }.build()

        return GET(url, headers)
    }

    override fun latestUpdatesParse(response: Response): MangasPage = parseMangaPage(response)

    // =============================== Search =======================================
    override fun searchMangaRequest(page: Int, query: String, filters: FilterList): Request {
        val url = "$baseUrl/allcomic".toHttpUrl().newBuilder().apply {
            addQueryParameter("q", query)
            addQueryParameter("page", page.toString())

            filters.forEach { filter ->
                when (filter) {
                    is StatusFilter -> {
                        addQueryParameter("status", filter.toUriPart())
                    }
                    is SortFilter -> {
                        addQueryParameter("sort", filter.toUriPart())
                    }
                    is OrderFilter -> {
                        addQueryParameter("order", filter.toUriPart())
                    }
                    is TypeFilter -> {
                        addQueryParameter("comic_type", filter.toUriPart())
                    }
                    is GenreFilter -> {
                        addQueryParameter("genre", filter.toUriPart())
                    }
                    is ColorFilter -> {
                        addQueryParameter("color_format", filter.toUriPart())
                    }
                    is ReadingFormatFilter -> {
                        addQueryParameter("reading_format", filter.toUriPart())
                    }
                    is AuthorFilter -> {
                        addQueryParameter("author", filter.toUriPart())
                    }
                    is ArtistFilter -> {
                        addQueryParameter("artist", filter.toUriPart())
                    }
                    else -> {}
                }
            }
        }.build()

        return GET(url, headers)
    }

    override fun searchMangaParse(response: Response): MangasPage = parseMangaPage(response)

    // =============================== Manga Details ================================
    override fun getMangaUrl(manga: SManga): String = "$baseUrl/comic/${manga.url}"

    override fun mangaDetailsRequest(manga: SManga): Request = GET("$baseUrl/comic/${manga.url}", headers)

    override fun mangaDetailsParse(response: Response): SManga = SManga.create().apply {
        val document = response.asJsoup()

        title = document.selectFirst("h1.card-title, h1.text-dark")?.text() ?: ""

        thumbnail_url = document.selectFirst("img.card-img-top, img.img-fluid")?.absUrl("src")

        // Parse metadata from table or list
        document.select("div.card-body table tr, div.info-block").forEach { row ->
            val label = row.selectFirst("td:first-child, strong, .label")?.text()?.lowercase() ?: ""
            val value = row.selectFirst("td:last-child, span")?.text() ?: ""

            when {
                label.contains("author") -> author = value
                label.contains("artist") -> artist = value
                label.contains("status") -> status = parseStatus(value)
                label.contains("genre") -> genre = value
            }
        }

        description = document.selectFirst("div.synopsis, div.description, p:contains(Sinopsis)")?.text()

        initialized = true
    }

    private fun parseStatus(status: String): Int = when {
        status.contains("ongoing", ignoreCase = true) -> SManga.ONGOING
        status.contains("completed", ignoreCase = true) -> SManga.COMPLETED
        status.contains("hiatus", ignoreCase = true) -> SManga.ON_HIATUS
        status.contains("canceled", ignoreCase = true) || status.contains("cancelled", ignoreCase = true) -> SManga.CANCELLED
        else -> SManga.UNKNOWN
    }

    // =============================== Chapters =====================================
    override fun getChapterUrl(chapter: SChapter): String {
        val parts = chapter.url.split("/")
        return "$baseUrl/comic/${parts[0]}/chapter/${parts[2]}"
    }

    override fun chapterListRequest(manga: SManga): Request = mangaDetailsRequest(manga)

    override fun chapterListParse(response: Response): List<SChapter> {
        val document = response.asJsoup()
        val chapters = mutableListOf<SChapter>()

        // Parse chapters from HTML
        document.select("ul.chapter-list li, div.chapter-list a, a.chapter-item").forEach { element ->
            SChapter.create().apply {
                setUrlWithoutDomain(element.attr("href"))
                name = element.selectFirst(".chapter-name, span.title")?.text() ?: element.text()
                chapter_number = element.attr("data-num").toFloatOrNull() ?: -1f
                date_upload = element.selectFirst(".chapter-date, time")?.text()?.let { dateFormat.tryParse(it) } ?: 0L
                chapters.add(this)
            }
        }

        return chapters.reversed()
    }

    // =============================== Page List ====================================
    override fun pageListRequest(chapter: SChapter): Request = GET("$baseUrl/comic/${chapter.url}", headers)

    override fun pageListParse(response: Response): List<Page> {
        val document = response.asJsoup()
        val pages = mutableListOf<Page>()

        // Parse pages from chapter HTML
        document.select("div.chapter-content img, div.reading-content img").forEachIndexed { index, element ->
            pages.add(
                Page(
                    index,
                    "",
                    element.absUrl("src").ifEmpty { element.absUrl("data-src") },
                ),
            )
        }

        return pages
    }

    override fun imageUrlParse(response: Response): String = throw UnsupportedOperationException()

    override fun imageRequest(page: Page): Request {
        val newHeaders = headersBuilder()
            .set("Accept", "image/avif,image/webp,image/apng,image/svg+xml,image/*,*/*;q=0.8")
            .build()

        return GET(page.imageUrl!!, newHeaders)
    }

    // =============================== Utilities ====================================
    private fun parseMangaPage(response: Response): MangasPage {
        val document = response.asJsoup()

        // Parse manga list from web HTML
        val mangas = document.select("div.card-body a.text-dark").map { element ->
            SManga.create().apply {
                setUrlWithoutDomain(element.attr("href"))
                title = element.attr("title").ifBlank { element.text() }
            }
        }

        // Check for next page button
        val hasNextPage = document.select("li.page-item a[rel=next]").isNotEmpty() ||
            document.select("nav[aria-label=Pagination]").isNotEmpty()

        return MangasPage(mangas, hasNextPage)
    }

    private fun parseMangaFromElement(element: Element): SManga = SManga.create().apply {
        setUrlWithoutDomain(element.attr("href"))
        title = element.attr("title").ifBlank { element.text() }
        thumbnail_url = element.selectFirst("img")?.absUrl("src")
    }

    private fun formatChapterNumber(number: String): String = number.toFloatOrNull()?.let {
        chapterNumberFormatter.format(it)
    } ?: number

    private val chapterNumberFormatter = DecimalFormat("#.##", DecimalFormatSymbols(Locale.US))

    private val dateFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US)

    // =============================== Filters ======================================
    override fun getFilterList(): FilterList = FilterList(
        SortFilter(),
        OrderFilter(),
        StatusFilter(),
        TypeFilter(),
        GenreFilter(),
        ColorFilter(),
        ReadingFormatFilter(),
        AuthorFilter(),
        ArtistFilter(),
    )
}
