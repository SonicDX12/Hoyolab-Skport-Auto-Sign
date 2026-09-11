# HoYoLab & SKPort Auto Sign

Android app that performs the daily check-in for **HoYoLab** games and **Arknights: Endfield** (SKPort),
on a schedule, from your phone — no Google Apps Script, no server, no third party holding your cookies.

## Features

- **HoYoLab**: Genshin Impact, Honkai: Star Rail, Honkai Impact 3rd, Tears of Themis, Zenless Zone Zero
- **SKPort**: Arknights: Endfield daily attendance
- Multiple accounts, each with its own token and game selection
- Daily scheduling at a chosen time (WorkManager)
- In-app login WebView that extracts the needed cookies for you
- Android notifications and/or Discord webhook report of each run
- Tokens stored in `EncryptedSharedPreferences` (AES256-GCM, Android Keystore master key)

## Requirements

- Android 8.0 (API 26) or newer
- Android Studio (JDK 17, Android SDK 35) to build

## Build

```bash
git clone https://github.com/SonicDX12/Hoyolab-Skport-Auto-Sign.git
cd Hoyolab-Skport-Auto-Sign
./gradlew assembleDebug
```

Or just open the folder in Android Studio and run the `app` configuration.

`local.properties` is not in the repo — Android Studio writes it on first open, or create it yourself with
`sdk.dir=/path/to/Android/Sdk`.

## Setup

1. Add an account, pick the provider (HoYoLab or SKPort).
2. Log in through the built-in WebView — the cookies are captured automatically.
   You can also paste them manually:
   - **HoYoLab**: `ltoken_v2`, `ltuid_v2`, `account_mid_v2`, `account_id_v2`, `ltmid_v2` from `hoyolab.com`
   - **SKPort**: the `ACCOUNT_TOKEN` cookie from `.skport.com`
3. Tick the games to sign in for.
4. In Settings, enable the daily schedule and, optionally, a Discord webhook.

## Security

Your tokens are the equivalent of your session — treat them as passwords. They never leave the device
except in the sign-in requests to HoYoLab/SKPort (and, if you configure one, the Discord webhook you own).
Nothing sensitive is committed to this repository.

## Credits

The HoYoLab sign-in endpoints and headers follow the work in
[canaria3406/hoyolab-auto-sign](https://github.com/canaria3406/hoyolab-auto-sign).

## Disclaimer

Unofficial project, not affiliated with HoYoverse, Hypergryph or Gryphline. Use at your own risk.

## License

[MIT](LICENSE)
