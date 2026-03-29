package eu.kanade.tachiyomi.extension.id.yubikiri

import android.util.Base64
import eu.kanade.tachiyomi.multisrc.madara.Madara
import eu.kanade.tachiyomi.network.POST
import eu.kanade.tachiyomi.source.model.SChapter
import eu.kanade.tachiyomi.source.model.SManga
import eu.kanade.tachiyomi.util.asJsoup
import okhttp3.OkHttpClient
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.concurrent.TimeUnit

class Kaguya :
    Madara(
        "Kaguya",
        "https://v1.kaguya.pro",
        "id",
        dateFormat = SimpleDateFormat("d MMMM", Locale("en")),
    ) {

    override val client: OkHttpClient = super.client.newBuilder()
        .readTimeout(1, TimeUnit.MINUTES)
        .build()

    override val id = 1557304490417397104

    override val mangaSubString = "all-series"

    override val mangaDetailsSelectorTitle = "h1.post-title"
    override val mangaDetailsSelectorStatus = "div.summary-heading:contains(Status) + div"
    override val mangaDetailsSelectorThumbnail = "head meta[property='og:image']" // Same as browse

    override fun imageFromElement(element: Element): String? {
        if (element.hasAttr("data-aesir")) {
            val decoded = Base64.decode(element.attr("data-aesir"), Base64.DEFAULT).toString(Charsets.UTF_8).trim()
            if (decoded.isNotEmpty()) return decoded
        }

        return super.imageFromElement(element)
            ?.takeIf { it.isNotEmpty() }
            ?: element.attr("content") // Thumbnail from <head>
    }

    // ============================== Chapters ==============================
    override suspend fun getChapterList(manga: SManga): List<SChapter> {
        val chapters = mutableListOf<SChapter>()
        var page = 1
        while (true) {
            val response = kotlinx.coroutines.coroutineScope {
                client.newCall(POST("${getMangaUrl(manga)}ajax/chapters?t=${page++}", xhrHeaders)).execute()
            }
            if (!response.isSuccessful) {
                response.close()
                throw IllegalStateException("HTTP error ${response.code}")
            }
            val document = response.asJsoup()
            val currentPage = document.select(chapterListSelector())
                .map(::chapterFromElement)

            chapters += currentPage
            response.close()

            if (currentPage.isEmpty()) {
                break
            }
        }
        return chapters
    }

    // ============================== Filter ==============================
    override fun parseGenres(document: Document): List<Genre> = document.select("a.btn[href*=\"genre=\"], a.dropdown-item[href*=\"genre=\"]")
        .map { a ->
            Genre(
                a.text(),
                a.attr("href").substringAfter("genre=").substringBefore("&"),
            )
        }.distinctBy { it.id }

    override val useLoadMoreRequest = LoadMoreStrategy.Never
}
