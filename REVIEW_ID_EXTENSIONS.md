# Review Extensions Indonesia (src/id/)

## Ringkasan

- **Total Extensions**: 88 extensions
- **Bahasa**: Indonesia (id)
- **Status**: Sebagian besar aktif dan berfungsi

## Struktur Direktori

```
src/id/
├── <nama-sumber>/
│   ├── build.gradle
│   └── src/eu/kanade/tachiyomi/extension/id/<nama-sumber>/
│       ├── <NamaSumber>.kt
│       ├── Filters.kt (opsional)
│       └── Dto.kt (opsional, untuk serialisasi)
```

## Temuan Review

### ✅ **Praktik yang Baik (Best Practices)**

#### 1. **Komiku** - Contoh Implementasi HTTP Source yang Baik
- ✅ Menggunakan `HttpSource` dengan benar untuk API-based source
- ✅ Implementasi pagination yang tepat dengan `hasNextPage`
- ✅ Error handling untuk response 404
- ✅ Date parsing dengan format relatif ("x jam lalu", "x menit lalu")
- ✅ Filter implementation dengan `UriFilter` interface
- ✅ URL manipulation yang benar dengan `toHttpUrl()`

```kotlin
// Contoh bagus: Relative date parsing
private fun parseRelativeDate(date: String): Long {
    val trimmedDate = date.substringBefore(" lalu").removeSuffix("s").split(" ")
    val calendar = Calendar.getInstance()
    when (trimmedDate[1]) {
        "jam" -> calendar.add(Calendar.HOUR_OF_DAY, -trimmedDate[0].toInt())
        "menit" -> calendar.add(Calendar.MINUTE, -trimmedDate[0].toInt())
        "detik" -> calendar.add(Calendar.SECOND, 0)
    }
    return calendar.timeInMillis
}
```

#### 2. **KomikCast** - Implementasi API Modern dengan DTOs
- ✅ Menggunakan Kotlinx Serialization untuk JSON parsing
- ✅ DTO separation yang baik (SeriesItem, SeriesData, ChapterItem, dll)
- ✅ Rate limiting yang tepat (`rateLimit(3)`)
- ✅ Custom headers untuk API requirements
- ✅ Migration handling untuk URL changes

```kotlin
// Contoh bagus: DTO separation
@Serializable
data class SeriesListResponse(
    val data: List<SeriesItem>,
    val meta: Meta? = null,
)

@Serializable
data class SeriesItem(
    val id: Int,
    val data: SeriesData,
)
```

#### 3. **Shinigami** - API-based Source dengan Error Handling
- ✅ Custom interceptor untuk header manipulation
- ✅ Migration handling dari domain lama ke baru
- ✅ Error message yang informatif untuk user
- ✅ Rate limiting yang tepat
- ✅ Token-based authentication handling

```kotlin
// Contoh bagus: Migration handling
override fun mangaDetailsRequest(manga: SManga): Request {
    if (manga.url.startsWith("/series/")) {
        throw Exception("Migrate dari $name ke $name (ekstensi yang sama)")
    }
    return GET("$apiUrl/v1/manga/detail/${manga.url}", apiHeaders)
}
```

#### 4. **Softkomik** - Advanced Features
- ✅ Session management dengan caching
- ✅ Multiple CDN fallback handling
- ✅ Next.js data extraction (`extractNextJs`)
- ✅ Interceptor chaining untuk image dan API
- ✅ Complex filter implementation

```kotlin
// Contoh bagus: Session caching dengan thread safety
private fun getSession(): SessionDto {
    val currentSession = session
    if (currentSession != null && currentSession.ex > System.currentTimeMillis()) {
        return currentSession
    }

    synchronized(this) {
        val currentSessionSync = session
        if (currentSessionSync != null && currentSessionSync.ex > System.currentTimeMillis()) {
            return currentSessionSync
        }
        // ... fetch new session
    }
}
```

#### 5. **Mangasusu** - Multi-source Theme Implementation
- ✅ Menggunakan `MangaThemesia` base class dengan benar
- ✅ Sucuri bypass implementation dengan QuickJS
- ✅ Custom page parsing untuk TSReader
- ✅ Error handling untuk protected sites

```kotlin
// Contoh bagus: Sucuri bypass
private fun sucuriInterceptor(chain: Interceptor.Chain): Response {
    val request = chain.request()
    val response = try {
        chain.proceed(request)
    } catch (e: Exception) {
        // Clear cookies and retry
        client.cookieJar.saveFromResponse(url, emptyList())
        chain.proceed(request.newBuilder().headers(clearHeaders).build())
    }
    // ... Sucuri handling
}
```

#### 6. **DoujinDesu** - Configurable Source dengan Preferences
- ✅ Implementasi `ConfigurableSource` untuk user preferences
- ✅ Domain customization dengan `EditTextPreference`
- ✅ Random User-Agent dengan library `randomua`
- ✅ Complex filter dengan banyak genres
- ✅ Advanced HTML parsing dengan multiple cases

```kotlin
// Contoh bagus: Configurable source
override fun setupPreferenceScreen(screen: PreferenceScreen) {
    EditTextPreference(screen.context).apply {
        key = PREF_DOMAIN_KEY
        title = PREF_DOMAIN_TITLE
        summary = PREF_DOMAIN_SUMMARY
        setDefaultValue(PREF_DOMAIN_DEFAULT)
        setOnPreferenceChangeListener { _, newValue ->
            Toast.makeText(screen.context, "Restart aplikasi", Toast.LENGTH_LONG).show()
            true
        }
    }.also(screen::addPreference)
    screen.addRandomUAPreference()
}
```

#### 7. **Komikindo** - Multi-source Theme dengan Customization
- ✅ Menggunakan `MangaThemesia` dengan override yang tepat
- ✅ Custom interceptor untuk CDN headers
- ✅ Image URL manipulation untuk resize parameter

```kotlin
// Contoh bagus: CDN header handling
override val client = super.client.newBuilder()
    .rateLimit(3)
    .addInterceptor { chain ->
        val request = chain.request()
        val url = request.url.toString()
        if (url.contains("/wp-content/uploads/")) {
            return@addInterceptor chain.proceed(request.newBuilder().headers(cdnHeaders).build())
        }
        chain.proceed(request)
    }
    .build()
```

#### 8. **KlikManga** - Madara Theme Implementation
- ✅ Menggunakan `Madara` multisrc theme
- ✅ Custom date format untuk locale Indonesia
- ✅ LoadMore strategy configuration

```kotlin
class KlikManga : Madara(
    "KlikManga",
    "https://klikmanga.org",
    "id",
    SimpleDateFormat("MMMM dd, yyyy", Locale("id")),
) {
    override val mangaSubString = "daftar-komik"
    override val useLoadMoreRequest = LoadMoreStrategy.Always
}
```

---

### ⚠️ **Isu yang Ditemukan**

#### 1. **Hardcoded URLs tanpa Fallback**
**Problem**: Beberapa extensions menggunakan hardcoded URLs tanpa mekanisme fallback jika domain berubah.

**Contoh yang perlu diperbaiki**:
```kotlin
// ❌ Kurang baik
override val baseUrl = "https://komiku.org"

// ✅ Lebih baik
override val baseUrl by lazy { 
    preferences.getString(PREF_DOMAIN_KEY, PREF_DOMAIN_DEFAULT)!! 
}
```

**Rekomendasi**: Gunakan `ConfigurableSource` untuk extensions dengan domain yang sering berubah.

#### 2. **Missing Rate Limiting**
**Problem**: Tidak semua extensions mengimplementasikan rate limiting.

**Contoh yang perlu diperbaiki**:
```kotlin
// ❌ Kurang baik - tidak ada rate limit
override val client: OkHttpClient = network.cloudflareClient

// ✅ Lebih baik
override val client: OkHttpClient = network.cloudflareClient.newBuilder()
    .rateLimit(3)  // 3 requests per second
    .build()
```

**Extensions yang perlu ditambahkan rate limiting**:
- Komiku
- DoujinDesu
- Dan beberapa lainnya

#### 3. **Error Handling yang Kurang Informatif**
**Problem**: Beberapa extensions hanya throw exception tanpa pesan yang jelas.

**Contoh yang perlu diperbaiki**:
```kotlin
// ❌ Kurang baik
override fun pageListParse(response: Response): List<Page> {
    val data = response.extractNextJs<ChapterPageDataDto>()
        ?: throw Exception("Could not find chapter data")
}

// ✅ Lebih baik
override fun pageListParse(response: Response): List<Page> {
    val data = response.extractNextJs<ChapterPageDataDto>()
        ?: throw Exception("Gagal memuat data chapter. Coba buka di WebView atau periksa koneksi internet.")
}
```

#### 4. **Date Parsing tanpa Fallback**
**Problem**: Beberapa extensions tidak memiliki fallback untuk date parsing yang gagal.

**Rekomendasi**: Selalu gunakan `runCatching` atau try-catch untuk date parsing:
```kotlin
// ✅ Lebih baik
private fun parseChapterDate(dateString: String): Long {
    if (dateString.isBlank()) return 0L
    return runCatching {
        dateFormat.tryParse(dateString)
    }.getOrNull() ?: 0L
}
```

#### 5. **Magic Numbers dan Strings**
**Problem**: Beberapa extensions menggunakan magic numbers/strings tanpa konstanta.

**Contoh yang perlu diperbaiki**:
```kotlin
// ❌ Kurang baik
.addQueryParameter("page_size", "30")
.addQueryParameter("take", "12")

// ✅ Lebih baik
companion object {
    private const val PAGE_SIZE = 30
    private const val ITEMS_PER_PAGE = 12
}
.addQueryParameter("page_size", PAGE_SIZE.toString())
```

#### 6. **Kotlin Style Issues**
**Problem**: Beberapa extensions tidak mengikuti Kotlin best practices.

**Contoh**:
```kotlin
// ❌ Kurang baik
if (status == null) SManga.UNKNOWN
else if (status.contains("Ongoing", true)) SManga.ONGOING
else if (status.contains("End", true)) SManga.COMPLETED
else SManga.UNKNOWN

// ✅ Lebih baik
when {
    status == null -> SManga.UNKNOWN
    status.contains("Ongoing", true) || status.contains("On Going", true) -> SManga.ONGOING
    status.contains("End", true) || status.contains("Completed", true) -> SManga.COMPLETED
    else -> SManga.UNKNOWN
}
```

#### 7. **Missing Documentation**
**Problem**: Kurangnya KDoc untuk fungsi-fungsi kompleks.

**Rekomendasi**: Tambahkan KDoc untuk fungsi yang kompleks:
```kotlin
/**
 * Parse relative date string from Indonesian format.
 * Supports formats like "5 jam lalu", "30 menit lalu", "1 detik lalu".
 * 
 * @param date The relative date string to parse
 * @return Unix timestamp in milliseconds, or current time for recent items
 */
private fun parseRelativeDate(date: String): Long {
    // ...
}
```

#### 8. **Inconsistent Naming**
**Problem**: Beberapa extensions menggunakan naming yang tidak konsisten.

**Contoh**:
- `mangaListParse()` vs `parseSeriesListResponse()` vs `popularMangaParse()`
- `chapterFromObject()` vs `chapterListParse()`

**Rekomendasi**: Ikuti naming convention dari base class:
- `popularMangaParse()`
- `latestUpdatesParse()`
- `searchMangaParse()`
- `mangaDetailsParse()`
- `chapterListParse()`
- `pageListParse()`

---

### 🔧 **Rekomendasi Perbaikan**

#### **Prioritas Tinggi**

1. **Tambahkan Rate Limiting** ke semua extensions yang belum memiliki
   - Target: ~20 extensions
   - Impact: Mencegah IP banning dari server source

2. **Implementasi Error Messages dalam Bahasa Indonesia**
   - Ganti error messages English dengan Indonesia
   - Impact: User experience lebih baik untuk target audience

3. **Tambahkan Configurable Domain** untuk extensions dengan domain tidak stabil
   - Target: ~15 extensions
   - Impact: Mengurangi maintenance saat domain berubah

#### **Prioritas Sedang**

4. **Standardisasi DTO/Response Models**
   - Gunakan Kotlinx Serialization secara konsisten
   - Pisahkan request/response models dengan jelas

5. **Tambahkan KDoc untuk Fungsi Kompleks**
   - Terutama untuk date parsing, URL manipulation, dan filter handling

6. **Refactor Magic Numbers/Strings**
   - Pindahkan ke companion object constants

#### **Prioritas Rendah**

7. **Konsistensi Naming Convention**
   - Unifikasi naming untuk fungsi-fungsi serupa

8. **Code Formatting dengan Spotless**
   - Pastikan semua extensions mengikuti format yang sama

---

## **Extensions dengan Kualitas Terbaik** ⭐

Berikut adalah extensions dengan implementasi terbaik:

| Rank | Extension | Alasan |
|------|-----------|--------|
| 1 | **Softkomik** | Session management, CDN fallback, Next.js extraction, comprehensive filters |
| 2 | **KomikCast** | Clean DTOs, proper serialization, migration handling, good error messages |
| 3 | **Shinigami** | API-based dengan auth, migration handling, rate limiting, custom interceptors |
| 4 | **Komiku** | Clean HTTP source implementation, relative date parsing, good filter handling |
| 5 | **DoujinDesu** | Configurable source, complex HTML parsing, random UA, preference handling |

---

## **Extensions yang Perlu Refactoring** ⚠️

| Extension | Masalah Utama | Rekomendasi |
|-----------|---------------|-------------|
| DoujinDesu | Terlalu kompleks, banyak hardcoded logic | Split menjadi beberapa file, extract helper functions |
| Beberapa extensions kecil | Tidak ada rate limiting | Tambahkan `.rateLimit(3)` ke client |
| Extensions dengan hardcoded URL | Domain bisa berubah | Tambahkan ConfigurableSource |

---

## **Statistik Extensions**

### Berdasarkan Tipe Implementation

| Tipe | Jumlah | Persentase |
|------|--------|------------|
| ParsedHttpSource | ~35 | 40% |
| HttpSource | ~40 | 45% |
| Multi-source Theme | ~13 | 15% |

### Berdasarkan Fitur

| Fitur | Jumlah | Persentase |
|-------|--------|------------|
| Dengan Filters | ~60 | 68% |
| Dengan Latest Support | ~70 | 80% |
| API-based | ~25 | 28% |
| Configurable | ~5 | 6% |
| Multi-source Theme | ~13 | 15% |

---

## **Kesimpulan**

Secara keseluruhan, extensions Indonesia memiliki kualitas yang **baik** dengan beberapa highlight:

✅ **Kekuatan**:
- Implementasi API-based yang solid (Softkomik, KomikCast, Shinigami)
- Penggunaan multi-source themes yang efektif (Mangasusu, Komikindo, KlikManga)
- Error handling yang cukup baik
- Filter implementation yang comprehensive

⚠️ **Area untuk Improvement**:
- Konsistensi rate limiting
- Error messages dalam bahasa Indonesia
- Configurable domain untuk flexibility
- Documentation (KDoc)
- Code standardization

**Rekomendasi Utama**: Fokus pada penambahan rate limiting dan error messages yang lebih informatif sebagai prioritas utama.
