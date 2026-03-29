package eu.kanade.tachiyomi.extension.id.roseveil

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
class SearchResponseDto(
    val data: List<MangaItemDto> = emptyList(),
    val page: Int = 0,
    val limit: Int = 0,
    val total: Int = 0,
    @SerialName("total_pages") val totalPages: Int = 0,
)

@Serializable
class MangaItemDto(
    val title: String = "",
    val slug: String = "",
    @SerialName("poster_image_url") val posterImageUrl: String? = null,
)

@Serializable
class MangaDetailDto(
    val title: String = "",
    val slug: String = "",
    val synopsis: String? = null,
    @SerialName("poster_image_url") val posterImageUrl: String? = null,
    @SerialName("author_name") val authorName: String? = null,
    @SerialName("artist_name") val artistName: String? = null,
    @SerialName("comic_status") val comicStatus: String? = null,
    val genres: List<GenreDto> = emptyList(),
    val units: List<ChapterUnitDto> = emptyList(),
)

@Serializable
class GenreDto(
    val name: String = "",
)

@Serializable
class ChapterUnitDto(
    val title: String? = null,
    val slug: String = "",
    val number: String = "",
    @SerialName("created_at") val createdAt: String? = null,
)

@Serializable
class PageListDto(
    val chapter: ChapterDetailDto = ChapterDetailDto(),
)

@Serializable
class ChapterDetailDto(
    val pages: List<PageDto> = emptyList(),
)

@Serializable
class PageDto(
    @SerialName("page_number") val index: Int = 0,
    @SerialName("image_url") val url: String = "",
)
