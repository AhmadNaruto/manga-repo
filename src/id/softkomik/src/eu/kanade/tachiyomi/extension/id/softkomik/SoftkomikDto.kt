package eu.kanade.tachiyomi.extension.id.softkomik

import kotlinx.serialization.Serializable

// Use regular class instead of data class to reduce bytecode size
@Serializable
class LibDataDto(
    val data: List<MangaDto>,
    val maxPage: Int,
    val page: Int,
)

@Serializable
class MangaDto(
    val gambar: String,
    val title: String,
    val title_slug: String,
    val status: String? = null,
    val type: String? = null,
)

@Serializable
class MangaDetailsDto(
    val gambar: String,
    val title: String,
    val author: String? = null,
    val Genre: List<String>? = emptyList(),
    val sinopsis: String? = null,
    val status: String? = null,
    val type: String? = null,
)

@Serializable
class ChapterDto(
    val chapter: String,
)

@Serializable
class ChapterPageImagesDto(
    val imageSrc: List<String>,
)

@Serializable
class ChapterPageDataDto(
    val _id: String,
    val imageSrc: List<String>,
    val storageInter2: Boolean? = false,
)

@Serializable
class ChapterListDto(
    val chapter: List<ChapterDto>,
)

@Serializable
class SessionDto(
    val ex: Long,
    val sign: String,
    val token: String,
)

@Serializable
class BearerTokenDto(
    val token: String,
    val ex: Long,
)
