# Duck Viewer

Shows DuckDuckGo search results on an Android phone that has no web browser installed.

**Status: early development.**

## Why

With Chrome disabled and no other browser installed, tapping a web search suggestion in a launcher like Niagara does nothing, because nothing can open the link. Duck Viewer registers as the default browser, takes the search, and shows DuckDuckGo's results page in a bare WebView. It has no tabs, no address bar, and no bookmarks.

It renders pages with Android System WebView, which keeps working when Chrome is disabled.

## What it handles

- Links (`VIEW` http/https). DuckDuckGo searches are rewritten to [DuckDuckGo Lite](https://lite.duckduckgo.com/lite/), and DuckDuckGo's `/l/?uddg=` result redirects are decoded. Other links open in the same view.
- Web searches (`WEB_SEARCH`) and selected text (`PROCESS_TEXT`).
- Opening the app from the launcher shows a search box.

Searches with bangs (`!w kotlin`) and image, news, or video searches go to the full duckduckgo.com site, which handles them.

## Install

1. Download `duck-viewer-<version>.apk` from the [latest release](https://github.com/yusufaf/duck-viewer/releases/latest) on the phone.
2. Allow your Files app to install unknown apps when prompted.
3. Open Duck Viewer once and accept the default browser prompt. If there's no prompt, use the button to open Settings > Default apps.
4. In Niagara, turn on Settings > Smart Search > Web Search Suggestions with DuckDuckGo selected.

Keep Android System WebView enabled and updated from the Play Store.

## Release signing (one-time setup)

Each release must be signed with the same key, or Android refuses to install it over the previous version. Generate the key once and store it as repository secrets:

```powershell
keytool -genkeypair -v -keystore duck-viewer.jks -alias duckviewer -keyalg RSA -keysize 4096 -validity 36500
[Convert]::ToBase64String([IO.File]::ReadAllBytes("$PWD\duck-viewer.jks")) | gh secret set KEYSTORE_BASE64 -R yusufaf/duck-viewer
gh secret set KEYSTORE_PASSWORD -R yusufaf/duck-viewer
gh secret set KEY_ALIAS -R yusufaf/duck-viewer --body duckviewer
gh secret set KEY_PASSWORD -R yusufaf/duck-viewer
```

Back up `duck-viewer.jks` and its passwords somewhere safe, outside the repo. If the key is lost, the next version can only be installed after uninstalling the old one.

To build a signed release locally, set `KEYSTORE_FILE`, `KEYSTORE_PASSWORD`, `KEY_ALIAS` and `KEY_PASSWORD` in the environment. Without them, `assembleRelease` produces an unsigned APK.

## Building

```
./gradlew :app:testDebugUnitTest :app:assembleDebug
adb install app/build/outputs/apk/debug/app-debug.apk
```

CI builds every PR and push to `main`. Releases come from release-please; each release attaches the signed APK.

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
