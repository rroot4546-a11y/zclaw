# zclaw

Offline-first AI chat for Android. Everything runs on-device.

## Repo layout

- `android/` — Android APK (Kotlin + Gradle). Embeds a Termux-style Linux
  bootstrap, runs the Codex server locally, and presents the chat UI in a
  WebView. Includes an engine picker (Codex / OpenCode / OpenClaw).
- `web/` — the chat web UI (Vue + Vite) served by the local Codex server.

## Tracking

Prebuilt APKs and the embedded engine pack are published to GitHub Releases:

https://github.com/rroot4546-a11y/zclaw-builds/releases

- `zclaw-v*.apk` — the full app (includes the engines)
- `engine.zip` — the OpenCode engine pack (glibc runtime + opencode binary)

## Build (android)

The app is built on a Linux host with JDK17 + Android SDK build-tools 35.

1. Build the web UI and copy it into the app assets:

   ```bash
   cd web && npm install && npm run build
   cp -r dist ./../android/app/src/main/assets/server-bundle
   cp -r dist-cli ./../android/app/src/main/assets/server-bundle
   ```

2. Place the OpenCode engine pack at `android/app/src/main/assets/opencode/engine.zip`
   (see Release `engine.zip`), and the bootstrap archive
   `bootstrap-aarch64.zip` under `android/app/src/main/assets/`.

3. Build the APK:

   ```bash
   cd android
   export JAVA_HOME=/usr/lib/jvm/java-17-openjdk-amd64
   ./gradlew :app:assembleRelease -x lint
   ```

4. Align and sign:

   ```bash
   zipalign -f 4 app/build/outputs/apk/release/app-release-unsigned.apk out.apk
   apksigner sign --ks <keystore> --ks-key-alias zclaw --out zclaw.apk out.apk
   ```

## Targets

- Codex — OpenAI coding agent, on-device, working.
- OpenCode — agentic coding terminal, runs via a bundled glibc runtime under
  proot. Needs your own OpenAI API key (stored locally).
- OpenClaw — personal home assistant; coming soon.