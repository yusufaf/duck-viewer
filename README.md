# Duck Viewer

Shows DuckDuckGo search results on an Android phone that has no web browser installed, so web search suggestions in [Niagara Launcher](https://niagaralauncher.com/) open instead of doing nothing.

**Status:** working. Signed APKs are on the [releases page](https://github.com/yusufaf/duck-viewer/releases).

## Why

With Chrome disabled and no other browser installed, tapping a web search suggestion in a launcher like Niagara does nothing, because nothing can open the link. Duck Viewer registers as the default browser, takes the search, and shows DuckDuckGo's results page in a bare WebView. It has no tabs, no address bar, and no bookmarks.

It renders pages with Android System WebView, which keeps working when Chrome is disabled.

## What it handles

- Links (`VIEW` http/https). DuckDuckGo searches are rewritten to [DuckDuckGo Lite](https://lite.duckduckgo.com/lite/), and DuckDuckGo's `/l/?uddg=` result redirects are decoded. Other links open in the same view.
- Web searches (`WEB_SEARCH`) and selected text (`PROCESS_TEXT`).
- Opening the app from the launcher shows a search box.

Searches with bangs (`!w kotlin`) and image, news, or video searches go to the full duckduckgo.com site, which handles them.

## Install

[<img src="docs/badge_obtainium.png" alt="Get it on Obtainium" height="48">](https://apps.obtainium.imranr.dev/redirect?r=obtainium://add/https://github.com/yusufaf/duck-viewer)

[Obtainium](https://github.com/ImranR98/Obtainium) installs it from this repo's releases and checks for updates. Then continue from step 3 below.

To install by hand:

1. Download `duck-viewer-<version>.apk` from the [latest release](https://github.com/yusufaf/duck-viewer/releases/latest) on the phone.
2. Allow your Files app to install unknown apps when prompted.
3. Open Duck Viewer once and accept the default browser prompt. If there's no prompt, use the button to open Settings > Default apps.
4. In Niagara, turn on Settings > Smart Search > Web Search Suggestions with DuckDuckGo selected.

Keep Android System WebView enabled and updated from the Play Store.

## Building

```
./gradlew :app:testDebugUnitTest :app:assembleDebug
adb install app/build/outputs/apk/debug/app-debug.apk
```

The debug build installs as a separate app, `dev.yusufaf.duckviewer.debug` ("Duck Viewer (debug)"), next to the release one, so testing never replaces the signed install or its default-browser role. To send it an intent directly, add `-n dev.yusufaf.duckviewer.debug/dev.yusufaf.duckviewer.MainActivity` to the `adb shell am start` commands below.

CI builds every PR and push to `main`. Releases come from release-please; each release attaches the signed APK. Signing setup for maintainers is in [CONTRIBUTING.md](CONTRIBUTING.md).

## Testing with adb

```
adb shell am start -a android.intent.action.VIEW -d "https://duckduckgo.com/?q=portland+weather"
adb shell am start -a android.intent.action.WEB_SEARCH --es query "portland weather"
adb shell am start -a android.intent.action.VIEW -d "https://example.com"
adb logcat -s DuckViewer
```

Debug builds log every incoming intent (action, data, extras) under the `DuckViewer` tag.

## Tech

Kotlin, a single Activity with Android Views, AndroidX WebKit. minSdk 29 (for `RoleManager`), targetSdk 37. Only the `INTERNET` permission.

## License

[MIT](LICENSE)
