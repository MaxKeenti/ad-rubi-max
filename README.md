# UPIICSA Programacion Movil Workspace

This repository is organized by program so each delivery keeps its own code,
Firebase backend, tests, and documentation.

## Layout

```text
.
|-- Programa1/  # Mangos USA mobile purchase tracking app
|-- Programa2/  # AnimoChat student motivational AI chat planning
`-- Programa3/  # BacheWatch citizen pothole reporting
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

Program 2 has a buildable Android Compose scaffold. No chat UI, AI provider,
repository contract, safety classifier, manual runbook, or screenshots have
been implemented yet. Its docs mirror the Program 1 planning conventions:

```sh
ls Programa2/docs
```

See `Programa2/README.html` and `Programa2/docs/implementation_plan.html` for
the current Program 2 plan and scaffold-only implementation status.

## Programa3

BacheWatch: citizen pothole reporting with photo evidence, automatic
geolocation, and a severity-weighted incidence heatmap. Planning was
completed in the 2026-06-11 grilling session; development runs June 11–15.
Entrega deliverables will be HTML (mirroring Programa2's format).

See `Programa3/README.md` and `Programa3/docs/implementation_plan.md`.
