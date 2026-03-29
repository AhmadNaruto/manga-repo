# 📖 RxJava to Kotlin Coroutines Migration Guide

**Untuk Extension Developers**

---

## 🎯 Ringkasan

Project extensions telah memulai migrasi dari **RxJava Observable** ke **Kotlin Coroutines** untuk meningkatkan konsistensi dan maintainability kode.

**Status**: ✅ 4 extensions Indonesia berhasil dimigrate (March 29, 2026)

---

## 📋 Apa yang Berubah?

### Before (RxJava)
```kotlin
import rx.Observable

class MyExtension : HttpSource() {
    override fun fetchSearchManga(
        page: Int,
        query: String,
        filters: FilterList
    ): Observable<MangasPage> =
        client.newCall(request)
            .asObservableSuccess()
            .map { response -> parseResponse(response) }
}
```

### After (Coroutines)
```kotlin
import eu.kanade.tachiyomi.network.awaitSuccess

class MyExtension : HttpSource() {
    override suspend fun getSearchManga(
        page: Int,
        query: String,
        filters: FilterList
    ): MangasPage {
        val response = client.newCall(request).awaitSuccess()
        return parseResponse(response)
    }
}
```

---

## 🔍 Mengapa Migrasi Ini Penting?

1. **Konsistensi** - Semua extensions menggunakan pola async yang sama
2. **Readability** - Kode lebih mudah dibaca dan dipahami
3. **Maintainability** - Lebih mudah untuk maintenance dan debugging
4. **Modern Kotlin** - Mengikuti best practices Kotlin modern
5. **Error Handling** - Error handling lebih straightforward dengan coroutines

---

## 📝 Pattern Migrasi

### Pattern 1: fetchSearchManga (Paling Umum)

#### Sebelum
```kotlin
override fun fetchSearchManga(
    page: Int,
    query: String,
    filters: FilterList
): Observable<MangasPage> =
    client.newCall(searchMangaRequest(page, query, filters))
        .asObservableSuccess()
        .map { response -> searchMangaParse(response) }
```

#### Sesudah
```kotlin
override suspend fun getSearchManga(
    page: Int,
    query: String,
    filters: FilterList
): MangasPage {
    val response = client.newCall(searchMangaRequest(page, query, filters))
        .awaitSuccess()
    return searchMangaParse(response)
}
```

**Perubahan**:
- `fun` → `suspend fun`
- `fetch...` → `get...` (konvensi nama baru)
- `Observable<MangasPage>` → `MangasPage`
- `.asObservableSuccess().map { }` → `.awaitSuccess()` langsung
- Remove `import rx.Observable`
- Add `import eu.kanade.tachiyomi.network.awaitSuccess`

---

### Pattern 2: fetchChapterList dengan Loop

#### Sebelum
```kotlin
override fun fetchChapterList(manga: SManga): Observable<List<SChapter>> =
    Observable.fromCallable { fetchAllChapters(manga) }

private fun fetchAllChapters(manga: SManga): List<SChapter> {
    val chapters = mutableListOf<SChapter>()
    var page = 1
    while (true) {
        val response = client.newCall(request).execute()
        val currentPage = parseChapters(response)
        chapters += currentPage
        if (currentPage.isEmpty()) return chapters
    }
}
```

#### Sesudah
```kotlin
override suspend fun getChapterList(manga: SManga): List<SChapter> =
    fetchAllChapters(manga)

private suspend fun fetchAllChapters(manga: SManga): List<SChapter> {
    val chapters = mutableListOf<SChapter>()
    var page = 1
    while (true) {
        val response = client.newCall(request).awaitSuccess()
        val currentPage = parseChapters(response)
        chapters += currentPage
        if (currentPage.isEmpty()) return chapters
    }
}
```

**Perubahan**:
- `Observable.fromCallable { }` → direct suspend call
- `.execute()` → `.awaitSuccess()`
- `fun` → `suspend fun`
- Remove `import rx.Observable`

---

### Pattern 3: fetchPopularManga / fetchLatestManga

#### Sebelum
```kotlin
override fun fetchPopularManga(page: Int): Observable<MangasPage> =
    client.newCall(popularMangaRequest(page))
        .asObservableSuccess()
        .map { response -> popularMangaParse(response) }
```

#### Sesudah
```kotlin
override suspend fun getPopularManga(page: Int): MangasPage {
    val response = client.newCall(popularMangaRequest(page))
        .awaitSuccess()
    return popularMangaParse(response)
}
```

---

## ✅ Checklist Migrasi

Untuk setiap extension yang dimigrate:

### 1. Update Imports
- [ ] ❌ Hapus: `import rx.Observable`
- [ ] ❌ Hapus: `import rx.schedulers.Schedulers` (jika ada)
- [ ] ✅ Tambah: `import eu.kanade.tachiyomi.network.awaitSuccess`

### 2. Update Method Signature
- [ ] `fun` → `suspend fun`
- [ ] `fetch...` → `get...` (optional tapi recommended)
- [ ] `Observable<T>` → `T`

### 3. Update Method Body
- [ ] `.asObservableSuccess()` → `.awaitSuccess()`
- [ ] `.map { }` → langsung return value
- [ ] `.fromCallable { }` → direct call
- [ ] `.execute()` → `.awaitSuccess()`

### 4. Testing
- [ ] Build: `./gradlew :src:<lang>:<extension>:assemble`
- [ ] Test Popular manga
- [ ] Test Latest manga
- [ ] Test Search
- [ ] Test Manga details
- [ ] Test Chapter list
- [ ] Test Page images

---

## 🚀 Contoh Lengkap: Migrasi Extension

### File: `src/id/myextension/src/.../MyExtension.kt`

#### SEBELUM (masih pakai RxJava)
```kotlin
package eu.kanade.tachiyomi.extension.id.myextension

import eu.kanade.tachiyomi.network.GET
import eu.kanade.tachiyomi.network.asObservableSuccess
import eu.kanade.tachiyomi.source.model.MangasPage
import eu.kanade.tachiyomi.source.model.SManga
import eu.kanade.tachiyomi.source.online.HttpSource
import okhttp3.Request
import okhttp3.Response
import rx.Observable

class MyExtension : HttpSource() {
    override val name = "My Extension"
    override val baseUrl = "https://example.com"
    override val lang = "id"
    override val supportsLatest = true

    override fun popularMangaRequest(page: Int): Request = GET("$baseUrl/popular?page=$page", headers)

    override fun popularMangaParse(response: Response): MangasPage {
        // parsing logic
    }

    override fun fetchPopularManga(page: Int): Observable<MangasPage> =
        client.newCall(popularMangaRequest(page))
            .asObservableSuccess()
            .map { response -> popularMangaParse(response) }

    // ... other methods
}
```

#### SESUDAH (sudah migrate ke Coroutines)
```kotlin
package eu.kanade.tachiyomi.extension.id.myextension

import eu.kanade.tachiyomi.network.GET
import eu.kanade.tachiyomi.network.awaitSuccess
import eu.kanade.tachiyomi.source.model.MangasPage
import eu.kanade.tachiyomi.source.model.SManga
import eu.kanade.tachiyomi.source.online.HttpSource
import okhttp3.Request
import okhttp3.Response

class MyExtension : HttpSource() {
    override val name = "My Extension"
    override val baseUrl = "https://example.com"
    override val lang = "id"
    override val supportsLatest = true

    override fun popularMangaRequest(page: Int): Request = GET("$baseUrl/popular?page=$page", headers)

    override fun popularMangaParse(response: Response): MangasPage {
        // parsing logic
    }

    override suspend fun getPopularManga(page: Int): MangasPage {
        val response = client.newCall(popularMangaRequest(page)).awaitSuccess()
        return popularMangaParse(response)
    }

    // ... other methods
}
```

---

## ⚠️ Common Pitfalls

### 1. Lupa Tambah `suspend`
```kotlin
// ❌ SALAH
override fun getSearchManga(...): MangasPage {
    val response = client.newCall(request).awaitSuccess() // Error!
    return parseResponse(response)
}

// ✅ BENAR
override suspend fun getSearchManga(...): MangasPage {
    val response = client.newCall(request).awaitSuccess()
    return parseResponse(response)
}
```

### 2. Masih Pakai `.map` dari RxJava
```kotlin
// ❌ SALAH
override suspend fun getSearchManga(...): MangasPage {
    return client.newCall(request)
        .awaitSuccess()
        .map { response -> parseResponse(response) } // Error!
}

// ✅ BENAR
override suspend fun getSearchManga(...): MangasPage {
    val response = client.newCall(request).awaitSuccess()
    return parseResponse(response)
}
```

### 3. Tidak Handle Exception
```kotlin
// ❌ KURANG BAIK
override suspend fun getSearchManga(...): MangasPage {
    val response = client.newCall(request).awaitSuccess()
    return parseResponse(response)
}

// ✅ LEBIH BAIK
override suspend fun getSearchManga(...): MangasPage {
    return try {
        val response = client.newCall(request).awaitSuccess()
        parseResponse(response)
    } catch (e: Exception) {
        throw Exception("Gagal mengambil data: ${e.message}")
    }
}
```

---

## 🧪 Testing Setelah Migrasi

### Build Test
```bash
# Build extension
./gradlew :src:id:<extension-name>:assemble

# Build debug
./gradlew :src:id:<extension-name>:assembleDebug
```

### Runtime Test
1. Install extension di app Komikku/Mihon
2. Test setiap fitur:
   - ✅ Browse/Popular
   - ✅ Latest (jika supported)
   - ✅ Search dengan query
   - ✅ Search dengan filters
   - ✅ Manga details
   - ✅ Chapter list
   - ✅ Page images

---

## 📚 Resources

### Dokumentasi
- [Kotlin Coroutines Guide](https://kotlinlang.org/docs/coroutines-guide.html)
- [Kotlinx Coroutines](https://github.com/Kotlin/kotlinx.coroutines)
- [RxJava to Coroutines Mapping](https://github.com/Kotlin/kotlinx.coroutines/blob/master/reactive/coroutines-guide-reactive.md)

### Contoh Migrasi
- [NarasiNinja](../src/id/narasininja/src/eu/kanade/tachiyomi/extension/narasininja/NarasiNinja.kt)
- [MangaCan](../src/id/mangacan/src/eu/kanade/tachiyomi/extension/id/mangacan/MangaCan.kt)
- [DreamTeamsScans](../src/id/dreamteamsscans/src/eu/kanade/tachiyomi/extension/id/dreamteamsscans/DreamTeamsScans.kt)
- [Kaguya](../src/id/yubikiri/src/eu/kanade/tachiyomi/extension/id/yubikiri/Kaguya.kt)

---

## 🤔 FAQ

### Q: Apakah saya harus migrate sekarang?
**A**: Sangat disarankan untuk migrate sekarang agar kode lebih konsisten. Extensions lama masih akan tetap berfungsi karena backward compatibility.

### Q: Bagaimana jika extension saya kompleks?
**A**: Mulai dari method yang paling sederhana dulu (biasanya `fetchSearchManga`). Pattern migrasi sama untuk semua kasus.

### Q: Apakah ada breaking changes?
**A**: Tidak ada breaking changes untuk user akhir. Hanya perubahan internal di code structure.

### Q: Bagaimana dengan dependencies?
**A**: RxJava masih tersedia untuk backward compatibility. Setelah semua extensions migrate, RxJava akan dihapus dari dependencies.

### Q: Saya menemukan bug setelah migrate!
**A**: Silakan buat issue di GitHub dengan detail extension dan error yang terjadi.

---

## 📊 Progress Migrasi

| Language | Total Extensions | Migrated | Progress |
|----------|-----------------|----------|----------|
| **id** (Indonesian) | 88 | 4 | 4.5% |
| **en** (English) | ~415 | 0 | 0% |
| **zh** (Chinese) | ~200 | 0 | 0% |
| **ko** (Korean) | ~100 | 0 | 0% |
| **Other** | ~150 | 0 | 0% |
| **TOTAL** | **~953** | **4** | **0.4%** |

---

## 🎯 Next Steps

1. ✅ Migrate 4 Indonesian extensions (DONE)
2. ⏳ Test all 4 extensions in app
3. ⏳ Migrate English extensions (batch by batch)
4. ⏳ Migrate other languages
5. ⏳ Remove RxJava dependency completely

---

**Last Updated**: March 29, 2026  
**Status**: 🟡 In Progress  
**Contact**: Discord server / GitHub issues
