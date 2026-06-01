# UPIICSA Programacion Movil Workspace

This repository is organized by program so each delivery keeps its own code,
Firebase backend, tests, and documentation.

## Layout

```text
.
`-- Programa1/  # Mangos USA mobile purchase tracking app
```

Future work should be added as sibling directories, for example
`Programa2/`, with its own `docs/`, app code, backend config, and tests.

## Programa1

```sh
cd Programa1
./gradlew :app:assembleDebug
npm --prefix functions run build
firebase emulators:exec --only firestore "npm --prefix tests/rules test"
```

See `Programa1/README.md` for the full project details and delivery docs.
