# Handoff para Melanie - BacheWatch

**Fecha:** 2026-06-12  
**Release a probar:** App Distribution release `31cvis3ha3p2o`  
**APK de respaldo:** `docs/entrega/app-release.apk`

## Estado listo para pruebas

- El APK de release esta firmado con `~/keystores/bachewatch-release.jks`.
- El APK firmado fue copiado a `docs/entrega/app-release.apk`.
- El release fue subido a Firebase App Distribution y distribuido al grupo
  `testers`.
- Firestore rules, Storage rules e indices fueron desplegados al proyecto
  `bachewatch-upiicsa`.
- Anonymous Auth responde correctamente en el proyecto real.
- La API key de Google Maps `apikey bachewatch` tiene el SHA-1 de debug y
  el SHA-1 de release para `com.example.bachewatch`.

## Link de instalacion

Usar este release especifico:

```text
https://appdistribution.firebase.google.com/testerapps/1:1008157923999:android:2a6d136350f1b16bcee0aa/releases/31cvis3ha3p2o?utm_source=firebase-tools
```

Si App Distribution no muestra BacheWatch, Max debe agregar el correo
Google de Melanie al grupo `testers`:

```bash
firebase appdistribution:testers:add MELANIE_GOOGLE_EMAIL \
  --group-alias testers \
  --project bachewatch-upiicsa
```

## Pruebas que debe ejecutar Melanie

1. Instalar el release de App Distribution con la misma cuenta Google del
   telefono.
2. Ejecutar completa `docs/guia-pruebas-melanie.md`.
3. Guardar capturas en `docs/screenshots/` con los nombres indicados en
   la guia.
4. Re-ejecutar el release gate:

```bash
cd Programa3
./gradlew testDebugUnitTest
firebase emulators:exec --only firestore,storage "npm --prefix tests/rules test"
```

## Max ya verifico

- `./gradlew clean assembleRelease` pasa.
- `apksigner verify --print-certs docs/entrega/app-release.apk` confirma
  firma v2 con SHA-1 de release
  `C3:B3:33:A7:CC:FE:36:41:B3:41:13:D8:7D:9F:18:91:B7:25:54:BF`.
- `./gradlew testDebugUnitTest` pasa.
- `firebase emulators:exec --only firestore,storage "npm --prefix tests/rules test"` pasa.

No habia telefono/emulador conectado en esta maquina al momento del
handoff, asi que el smoke test visual del mapa/camara queda como primer
paso de Melanie en dispositivo real.
