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

Known tester gotcha: if the App Distribution install stays stuck at
"downloading", Melanie must accept the invite with the same Google
account used by Google Play Services on the phone.
