# Contributing

Build and test steps are in the [README](README.md#building). PR titles follow [Conventional Commits](https://www.conventionalcommits.org/) (`fix: ...`, `feat: ...`), because release-please builds the changelog from them.

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
