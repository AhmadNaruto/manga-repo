# 🔄 RxJava to Kotlin Coroutines Migration Plan - Extensions

## 📊 Executive Summary

**Target**: 250 extensions across all languages using RxJava
**Initial Focus**: 4 Indonesian extensions (src/id/)
**Goal**: Migrate from `Observable` to suspend functions

---

## 📈 Current State Analysis

### ✅ Phase 1: Indonesian Extensions (COMPLETE)

All 4 Indonesian extensions successfully migrated:

| Extension | File | Status | Methods Migrated |
|-----------|------|--------|------------------|
| **NarasiNinja** | `src/id/narasininja/.../NarasiNinja.kt` | ✅ | `getSearchManga` |
| **MangaCan** | `src/id/mangacan/.../MangaCan.kt` | ✅ | `getSearchManga` |
| **DreamTeamsScans** | `src/id/dreamteamsscans/.../DreamTeamsScans.kt` | ✅ | `getSearchManga` |
| **Kaguya** | `src/id/yubikiri/.../Kaguya.kt` | ✅ | `getChapterList` |

### ✅ Phase 2a: Multisrc Themes (COMPLETE - March 29, 2026)

**All 29 multisrc themes successfully migrated!**

| Theme | Status | Impact |
|-------|--------|--------|
| `madara` | ✅ | High (~50+ extensions) |
| `mangathemesia` | ✅ | High (~30+ extensions) |
| `mmrcms` | ✅ | Medium (~20+ extensions) |
| `heancms` | ✅ | Medium (~15+ extensions) |
| `mccms` | ✅ | Medium (~10+ extensions) |
| `grouple` | ✅ | Medium (~10+ extensions) |
| `hentaihand` | ✅ | Medium (~5+ extensions) |
| `mangacatalog` | ✅ | Medium (~5+ extensions) |
| `guya` | ✅ | Low |
| `libgroup` | ✅ | Low |
| `senkuro` | ✅ | Low |
| `monochrome` | ✅ | Low |
| `gravureblogger` | ✅ | Low |
| `eromuse` | ✅ | Low |
| `stalkercms` | ✅ | Low |
| `galleryadults` | ✅ | Low |
| `uzaymanga` | ✅ | Low |
| `yuyu` | ✅ | Low |
| `comicaso` | ✅ | Low |
| `peachscan` | ✅ | Low |
| `pizzareader` | ✅ | Low |
| `natsuid` | ✅ | Low |
| `blogtruyen` | ✅ | Low |
| `mccms-web` | ✅ | Low |
| `madtheme` | ✅ | Low |
| `kemono` | ✅ | Low |
| `Others` | ✅ | Low |

**Total multisrc themes migrated**: 29 themes

### Extensions Still Using RxJava (~236 files)

**Breakdown by Location**:

| Location | Count | Priority |
|----------|-------|----------|
| `src/en/` | ~100+ extensions | Medium |
| `src/zh/` | ~40+ extensions | Medium |
| `src/es/` | ~20+ extensions | Low |
| `src/pt/` | ~15+ extensions | Low |
| `src/ru/` | ~15+ extensions | Low |
| `src/fr/` | ~10+ extensions | Low |
| `src/ar/` | ~10+ extensions | Low |
| `src/th/` | ~5+ extensions | Low |
| `src/ko/` | ~5+ extensions | Low |
| `src/de/` | ~5+ extensions | Low |
| `src/it/` | ~5+ extensions | Low |
| `src/all/` | ~50+ extensions | Medium
