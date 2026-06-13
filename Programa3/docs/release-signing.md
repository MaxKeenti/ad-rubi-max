# Release signing and App Distribution

Recovery note for Max. Secrets stay outside git; this file documents the
repeatable release path only.

## Local files

Keystore location:

```bash
~/keystores/bachewatch-release.jks
```

Generate it once:

```bash
mkdir -p ~/keystores
keytool -genkeypair \
  -v \
  -keystore ~/keystores/bachewatch-release.jks \
  -alias bachewatch-release \
  -keyalg RSA \
  -keysize 2048 \
  -validity 10000
```

`Programa3/local.properties` must contain these keys. Do not commit the
file.

```properties
sdk.dir=/Users/moonstone/Library/Android/sdk
MAPS_API_KEY=<android-restricted-maps-key>
RELEASE_STORE_FILE=~/keystores/bachewatch-release.jks
RELEASE_STORE_PASSWORD=<password>
RELEASE_KEY_ALIAS=bachewatch-release
RELEASE_KEY_PASSWORD=<password>
FIREBASE_APP_ID=<firebase-android-app-id>
```

The Gradle release signing config is conditional. If any `RELEASE_*`
property is absent, debug and test builds still work on other machines.

## Release SHA-1 trap

After creating the keystore, copy the SHA-1 into the Android restriction
for the Maps API key in Google Cloud Console. Without this, Melanie gets
a release APK with a blank map.

```bash
keytool -list -v \
  -keystore ~/keystores/bachewatch-release.jks \
  -alias bachewatch-release
```

Restriction values:

- Package name: `com.example.bachewatch`
- Certificate fingerprint: release SHA-1 from the command above

Current release key fingerprint (created 2026-06-12):

```text
C3:B3:33:A7:CC:FE:36:41:B3:41:13:D8:7D:9F:18:91:B7:25:54:BF
```

The Google API key named `apikey bachewatch` is restricted to
`maps-android-backend.googleapis.com` and now has both the debug SHA-1
and the release SHA-1 above for package `com.example.bachewatch`.

## Build and smoke test

```bash
./gradlew clean assembleRelease
adb install -r app/build/outputs/apk/release/app-release.apk
```

Smoke the installed release before distributing:

- Map tiles render.
- Anonymous session starts silently.
- `Reportar` opens the camera flow.
- `Enviar reporte` creates a visible marker.
- `Recientes`, `Confirmar`, `Eliminar`, and `Zonas` still work.

## Firebase App Distribution

Enable App Distribution in Firebase Console, add Melanie to tester group
`testers`, then upload with the Firebase CLI:

```bash
firebase appdistribution:distribute \
  app/build/outputs/apk/release/app-release.apk \
  --app "$FIREBASE_APP_ID" \
  --groups testers \
  --release-notes "BacheWatch release candidate: GPS, camera, map, recientes, confirmar, eliminar, heatmap."
```

Copy the same signed APK to `docs/entrega/app-release.apk` for the final
delivery fallback after the map is verified in the release build.

Current App Distribution release (use this one, not the earlier
same-version upload):

- Release ID: `31cvis3ha3p2o`
- Console:
  `https://console.firebase.google.com/project/bachewatch-upiicsa/appdistribution/app/android:com.example.bachewatch/releases/31cvis3ha3p2o?utm_source=firebase-tools`
- Tester link:
  `https://appdistribution.firebase.google.com/testerapps/1:1008157923999:android:2a6d136350f1b16bcee0aa/releases/31cvis3ha3p2o?utm_source=firebase-tools`

If Melanie is not already a tester, add her Google account to the
existing group:

```bash
firebase appdistribution:testers:add MELANIE_GOOGLE_EMAIL \
  --group-alias testers \
  --project bachewatch-upiicsa
```

Known tester gotcha: if the App Distribution install stays stuck at
"downloading", Melanie must accept the invite with the same Google
account used by Google Play Services on the phone.
