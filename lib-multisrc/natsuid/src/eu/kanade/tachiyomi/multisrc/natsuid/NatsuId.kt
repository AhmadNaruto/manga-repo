package eu.kanade.tachiyomi.multisrc.natsuid

import eu.kanade.tachiyomi.network.GET
import eu.kanade.tachiyomi.source.model.FilterList
import eu.kanade.tachiyomi.source.model.MangasPage
import eu.kanade.tachiyomi.source.model.Page
import eu.kanade.tachiyomi.source.model.SChapter
import eu.kanade.tachiyomi.source.model.SManga
import eu.kanade.tachiyomi.source.online.HttpSource
import eu.kanade.tachiyomi.util.asJsoup
import keiyoushi.utils.parseAs
import keiyoushi.utils.tryParse
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.brotli.BrotliInterceptor
import org.jsoup.Jsoup
import java.lang.UnsupportedOperationException
import java.text.SimpleDateFormat
import java.util.Locale
import kotlin.random.Random

// https://themesinfo.com/natsu_id-theme-wordpress-c8x1c Wordpress Theme Author "Dzul Qurnain"
abstract class NatsuId(
    override val name: String,
    override val lang: String,
    override val baseUrl: String,
    val dateFormat: SimpleDateFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US),
) : HttpSource() {

    override val supportsLatest: Boolean = true

    protected open fun OkHttpClient.Builder.customizeClient(): OkHttpClient.Builder = this

    final override val client: OkHttpClient = network.cloudflareClient.newBuilder()
        .customizeClient()
        // fix disk cache
        .apply {
            val index = networkInterceptors().indexOfFirst { it is BrotliInterceptor }
            if (index >= 0) interceptors().add(networkInterceptors().removeAt(index))
        }
        .build()

    override fun headersBuilder() = super.headersBuilder()
        .set("Referer", "$baseUrl/")

    override fun popularMangaRequest(page: Int) = searchMangaRequest(page, "", SortFilter.popular)

    override fun popularMangaParse(response: Response) = searchMangaParse(response)

    override fun latestUpdatesRequest(page: Int) = searchMangaRequest(page, "", SortFilter.latest)

    override fun latestUpdatesParse(response: Response) = searchMangaParse(response)

    override suspend fun getSearchManga(page: Int, query: String, filters: FilterList): MangasPage = if (query.startsWith("https://")) {
        deepLink(query)
    } else {
        super.getSearchManga(page, query, filters)
    }

    private suspend fun deepLink(url: String): MangasPage {
        val httpUrl = url.toHttpUrl()
        if (
            httpUrl.host == baseUrl.toHttpUrl().host &&
            httpUrl.pathSegments.size >= 2 &&
            httpUrl.pathSegments[0] == "manga"
        ) {
            val slug = httpUrl.pathSegments[1]
            val url = "$baseUrl/wp-json/wp/v2/manga".toHttpUrl().newBuilder()
                .addQueryParameter("slug[]", slug)
                .addQueryParameter("_embed", null)
                .build()

            val response = client.newCall(GET(url, headers)).execute()
            if (!response.isSuccessful) {
                response.close()
                throw Exception("HTTP error ${response.code}")
            }
            val manga = response.parseAs<List<Manga>>(transform = ::transformJsonResponse)[0]

            if (manga.embedded.getTerms("type").contains("Novel")) {
                throw Exception("Novels are not supported")
            }

            return MangasPage(listOf(manga.toSManga()), false)
        }

        throw Exception("Unsupported url")
    }

    private val descriptionIdRegex = Regex("""ID: (\d+)""")
    private fun getMangaId(manga: SManga): String = if (manga.url.startsWith("{")) {
        manga.url.parseAs<MangaUrl>().id.toString()
    } else if (descriptionIdRegex.containsMatchIn(manga.description?.trim().orEmpty())) {
        descriptionIdRegex.find(manga.description!!.trim())!!.groupValues[1]
    } else {
        val document = client.newCall(
            GET(getMangaUrl(manga), headers),
        ).execute().asJsoup()

        document.selectFirst("#gallery-list")!!.attr("hx-get")
            .substringAfter("manga_id=").substringBefore("&")
    }

    override fun mangaDetailsRequest(manga: SManga): Request {
        val id = getMangaId(manga)
        val appendId = !manga.url.startsWith("{")

        return GET("$baseUrl/wp-json/wp/v2/manga/$id?_embed#$appendId", headers)
    }

    override fun getMangaUrl(manga: SManga): String {
        val slug = if (manga.url.startsWith("{")) {
            manga.url.parseAs<MangaUrl>().slug
        } else {
            "$baseUrl${manga.url}".toHttpUrl().pathSegments[1]
        }

        return "$baseUrl/manga/$slug/"
    }

    override fun mangaDetailsParse(response: Response): SManga {
        val manga = response.parseAs<Manga>(transform = ::transformJsonResponse)
        val appendId = response.request.url.fragment == "true"

        return manga.toSManga(appendId)
    }

    override fun chapterListRequest(manga: SManga): Request {
        val id = getMangaId(manga)

        val url = "$baseUrl/wp-admin/admin-ajax.php".toHttpUrl().newBuilder()
            .addQueryParameter("manga_id", id)
            .addQueryParameter("page", "${Random.nextInt(99, 9999)}") // keep above 3 for loading hidden chapter
            .addQueryParameter("action", "chapter_list")
            .build()

        return GET(url, headers)
    }

    protected open val chapterListSelector = "div a:has(time)"
    protected open val chapterNameSelector = "span"
    protected open val chapterDateSelector = "time"
    protected open val chapterDateAttribute = "datetime"

    override fun chapterListParse(response: Response): List<SChapter> {
        val document = Jsoup.parseBodyFragment(response.body.string(), baseUrl)

        return document.select(chapterListSelector).map {
            SChapter.create().apply {
                setUrlWithoutDomain(it.absUrl("href"))
                name = it.selectFirst(chapterNameSelector)!!.ownText()
                date_upload = dateFormat.tryParse(
                    it.selectFirst(chapterDateSelector)?.attr(chapterDateAttribute),
                )
            }
        }
    }

    protected open val pageListSelector = "main .relative section > img"

    override fun pageListParse(response: Response): List<Page> {
        val document = response.asJsoup()

        return document.select(pageListSelector).mapIndexed { idx, img ->
            Page(idx, imageUrl = img.absUrl("src"))
        }
    }

    override fun imageUrlParse(response: Response): String = throw UnsupportedOperationException()

    protected open fun transformJsonResponse(responseBody: String): String = responseBody
}
