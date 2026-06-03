# UPIICSA Programacion Movil Workspace

This repository is organized by program so each delivery keeps its own code,
Firebase backend, tests, and documentation.

## Layout

```text
.
|-- Programa1/  # Mangos USA mobile purchase tracking app
`-- Programa2/  # AnimoChat student motivational AI chat planning
```

Future work should be added as sibling directories with their own `docs/`, app
code, backend config, and tests.

## Programa1

```sh
cd Programa1
./gradlew :app:assembleDebug
npm --prefix functions run build
firebase emulators:exec --only firestore "npm --prefix tests/rules test"
```

See `Programa1/README.md` for the full project details and delivery docs.

## Programa2

Program 2 is currently in planning. Its docs mirror the Program 1 planning
conventions:

```sh
ls Programa2/docs
```

See `Programa2/README.md` and `Programa2/docs/implementation_plan.md` for the
current Program 2 plan.
