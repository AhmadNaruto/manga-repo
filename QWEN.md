# Yūzōnō Manga Repository (manga-repo)

## Project Overview

This is a repository of **manga/manhwa/manhua extensions** compatible with **Komikku**, **Mihon**, and **Tachiyomi** - popular Android manga reader applications. The project is built with **Kotlin** and **Gradle**, following Android development conventions.

### Key Characteristics

- **Purpose**: Provides extension catalogs that allow manga reader apps to access content from various online sources
- **Architecture**: Modular Gradle project with each extension as a separate module
- **Technologies**: Kotlin, Android SDK, OkHttp, JSoup, kotlinx.serialization
- **License**: Apache License 2.0

### Repository Structure

```
manga-repo/
├── src/                    # Individual extension sources organized by language
│   ├── en/                 # English language extensions
│   ├── pt/                 # Portuguese language extensions
│   ├── es/                 # Spanish language extensions
│   └── ...                 # Other languages (all, ar, bg, ca, cs, de, fr, id, it, ja, ko, pl, ru, th, tr, uk, vi, zh)
├── lib/                    # Shared utility libraries for extensions
│   ├── dataimage/          # Base64 image data handling
│   ├── i18n/               # Internationalization support
│   ├── cryptoaes/          # AES decryption utilities
│   └── ...                 # Other utilities (unpacker, synchrony, lzstring, etc.)
├── lib-multisrc/           # Multi-source themes for common website frameworks
│   ├── madara/             # Madara WordPress theme
│   ├── foolslide/          # FoolSlide reader
│   └── ...                 # Other common frameworks
├── core/                   # Core utilities shared across all extensions
├── buildSrc/               # Build logic and custom Gradle plugins
├── gradle/                 # Gradle wrapper and version catalog
└── template/               # Extension templates for scaffolding new sources
```

### Extension Organization

Extensions are organized by language code under `src/<lang>/<source-name>/`:

- Each extension has its own Gradle module
- Extension naming: lowercase ASCII letters and digits only
- Package structure: `eu.kanade.tachiyomi.extension.<lang>.<source-name>`

## Building and Running

### Prerequisites

- **Android Studio** or IntelliJ IDEA with Android support
- **JDK 17+** (required for modern Android development)
- **Android SDK** with appropriate API levels
- **Git** (for version control)

### Build Commands

```bash
# Build all extensions (may take significant time)
./gradlew assembleRelease

# Build a specific extension
./gradlew :src:en:asurascans:assembleRelease

# Build debug variant
./gradlew assembleDebug

# Run lint checks
./gradlew lint

# Format code with Spotless
./gradlew spotlessApply

# Clean build
./gradlew clean
```

### Development Workflow

1. **Load specific extensions** - Edit `settings.gradle.kts` to include only needed modules:
   ```kotlin
   // Comment out loadAllIndividualExtensions()
   // loadIndividualExtension("en", "asurascans")
   ```

2. **Sparse checkout** (for large repo optimization):
   ```bash
   git sparse-checkout set --cone --sparse-index
   git sparse-checkout add buildSrc core gradle lib lib-multisrc
   git sparse-checkout add src/en/asurascans
   ```

### Testing

```bash
# Run tests (if available)
./gradlew test

# Run tests for specific module
./gradlew :src:en:asurascans:test
```

## Development Conventions

### Extension Structure

Each extension module follows this structure:

```
src/<lang>/<source-name>/
├── AndroidManifest.xml     # Optional (for deep linking)
├── build.gradle            # Extension configuration
├── res/                    # Resources (launcher icons)
│   └── mipmap-*/
│       └── ic_launcher.png
└── src/
    └── eu/kanade/tachiyomi/extension/<lang>/<source-name>/
        └── <SourceName>.kt         # Main extension class
        └── <SourceName>Dto.kt      # Data transfer objects (optional)
        └── <SourceName>Filters.kt  # Search filters (optional)
```

### build.gradle Configuration

```groovy
ext {
    extName = 'Display Name'
    extClass = '.SourceClassName'
    extVersionCode = 1
    isNsfw = true  // Optional, defaults to false
}

apply from: "$rootDir/common.gradle"
```

### Coding Style

- **Language**: Kotlin 2.3.0
- **Formatting**: Enforced via Spotless (run `./gradlew spotlessApply`)
- **Pre-commit hooks**: Configured via `.pre-commit-config.yaml`
  - End-of-file fixer
  - Trailing whitespace remover

### Key Dependencies

| Dependency | Purpose |
|------------|---------|
| `extensions-lib` | Core extension API interfaces |
| `okhttp` | HTTP client for web requests |
| `jsoup` | HTML parsing |
| `kotlinx-serialization` | JSON/protobuf serialization |
| `rxjava` | Reactive programming (legacy support) |
| `coroutines` | Async operations |

### Extension Development Guidelines

1. **Main Class**: Implement `SourceFactory` or extend `HttpSource`/`ParsedHttpSource`
2. **Required Fields**: `name`, `baseUrl`, `lang`, `id`
3. **Key Methods**:
   - `fetchPopularManga()` - Browse entry point
   - `fetchLatestManga()` - Latest updates (if `supportsLatest = true`)
   - `fetchSearchManga()` - Search functionality
   - `getMangaDetails()` - Manga metadata
   - `getChapterList()` - Chapter listing (sorted descending)
   - `getPageList()` - Chapter pages

### Multi-Source Themes

For sources using common frameworks (Madara, FoolSlide, etc.), use `lib-multisrc/`:

```groovy
ext {
    extName = 'Source Name'
    extClass = '.SourceClassName'
    themePkg = 'madara'  // Reference to multisrc theme
    overrideVersionCode = 1
}

apply from: "$rootDir/common.gradle"
```

### Code Quality

- **Lint**: Custom lint rules via `keiyoushi.lint` plugin
- **EditorConfig**: Enforced via `.editorconfig`
- **Pre-commit**: Automated formatting checks

## Additional Resources

- **Contributing Guide**: See [CONTRIBUTING.md](./CONTRIBUTING.md) for detailed extension development tutorial
- **Code of Conduct**: [CODE_OF_CONDUCT.md](./CODE_OF_CONDUCT.md)
- **Removed Sources**: [REMOVED_SOURCES.md](./REMOVED_SOURCES.md) - Sources that won't be added
- **Discord Community**: [Join server](https://discord.gg/85MZhUX688) for support

## Repository Metadata

- **Auto-sync**: Merges updates from Keiyoushi extensions every 6 hours
- **Version Format**: `1.4.<extVersionCode>` (e.g., 1.4.54)
- **Signing**: Release builds require `signingkey.jks` with environment variables

## Common Tasks

### Adding a New Extension

1. Create directory: `src/<lang>/<source-name>/`
2. Add `build.gradle` with extension config
3. Create source class in proper package structure
4. Add launcher icons in `res/mipmap-*/`
5. Test with `./gradlew :src:<lang>:<source-name>:assembleDebug`

### Updating an Extension

1. Increment `extVersionCode` in `build.gradle`
2. Make code changes
3. Run `./gradlew spotlessApply`
4. Test build: `./gradlew :src:<lang>:<source-name>:assembleDebug`

### Debugging Extensions

- **Logs**: Use Android Logcat with app filters
- **Network Inspection**: Use OkHttp logging interceptor or proxy tools
- **WebView**: Use "Open in WebView" feature for URL inspection
