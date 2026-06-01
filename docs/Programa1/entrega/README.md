# Entregables — Proyecto Final Programación Móvil

Carpeta de entregables formales para el profesor. Estructura: cada
deliverable vive en su propia subcarpeta con un `content.md` editable y un
`main.typ` que lo envuelve con la portada y los estilos institucionales.

## Filosofía del repo

- **El contenido vive en Markdown.** Se edita ahí. Se revisa ahí. Diffea
  limpio en git.
- **Typst es solo la cáscara de presentación.** Aplica portada,
  tipografía US-letter, numeración, codly para bloques de código y embebe
  los diagramas SVG.
- **El bridge MD→Typst es `cmarker`** (paquete `@preview/cmarker:0.1.6`).
  Si una sección se ve mal con cmarker, esa sección puntual se reescribe
  directamente en Typst o se pasa por pandoc — *solo esa*, no todas.
- **Diagramas:** se autoran en Mermaid (renderiza en GitHub), se exportan
  a SVG una vez, y se embeben con `#image(...)` en Typst. Los fuentes
  `.mmd` viven junto al SVG en `shared/diagrams/`.
- **No hay deduplicación de logos/portada-template.** Convención heredada
  de `docs---ingPruebas` (ADR-0001 de ese repo). No es deuda, es decisión.

## Layout

```
entrega/
├── README.md              ← este archivo
├── main.typ               ← compila toda la entrega a un solo PDF
├── justfile               ← `just bundle` compila `entrega-completa.pdf`
├── shared/
│   ├── portada-template.typ   ← función portada(...) reutilizable
│   ├── style.typ              ← preámbulo: fuentes, codly, cmarker, integrantes
│   ├── media/logos/           ← IPN_Logo.svg, UPIICSA_Logo.svg
│   └── diagrams/              ← .mmd fuente + .svg renderizado
├── 01-resumen-ejecutivo/
│   ├── content.md
│   └── main.typ
├── 02-requerimientos/
├── 03-arquitectura/
├── 04-modelo-de-datos/
├── 05-decisiones-arquitectonicas/
├── 06-glosario/
├── 07-manual-de-usuario/
├── 08-plan-de-pruebas/
└── 09-conclusiones/
```

## Prerequisitos

- **Typst** (CLI): `brew install typst`
- **Fuentes instaladas localmente:** `ITC Avant Garde Gothic`,
  `JetBrainsMono NFM`. Las mismas que ya usa `docs---ingPruebas`.
- **Mermaid CLI** (opcional, solo para regenerar SVG):
  `npm install -g @mermaid-js/mermaid-cli`. Alternativa sin instalación:
  pegar el `.mmd` en https://mermaid.live y exportar SVG manualmente.
- **just** (opcional, si se quiere `just build`): `brew install just`.
  Sin just, cada documento se compila con `typst compile main.typ`.

## Compilar

```sh
# PDF unico para entrega final
just bundle

# O simplemente:
just

# Entregables individuales, si se necesita revisar una seccion aislada
just build

# O uno por uno (desde la raíz de su carpeta)
# NOTA: --root es obligatorio porque main.typ importa /shared/style.typ
# (ruta absoluta). Sin --root, Typst bloquea el acceso por sandbox.
cd 03-arquitectura && typst compile --root .. main.typ
```

`just bundle` produce `entrega-completa.pdf` desde el `main.typ` de esta
carpeta, con una sola portada e índice. Cada subcarpeta conserva su
`main.typ` independiente para compilar secciones aisladas; `just build`
produce esos `main.pdf` junto a cada fuente.

## Mantener sincronizado con las fuentes en inglés

Cuando se edita `CONTEXT.md`, un ADR, `implementation_plan.md` o se hace
una nueva sesión de grilling, los entregables aquí derivan información de
esos archivos y se vuelven obsoletos. Para resincronizar:

```
/entrega-sync
```

Es un skill local del proyecto (vive en `.claude/skills/entrega-sync/`)
que detecta el drift, propone los cambios, pregunta solo sobre
traducciones ambiguas, aplica las ediciones y verifica que el árbol
`entrega/` siga compilando. Hacer la llamada en una sesión fresca de
Claude funciona — el skill carga el contexto que necesita.

## Fuente canónica de información

Los entregables citan y reproducen información que vive en otros lados
del repo:

- **Glosario / definiciones de dominio:** `docs/Programa1/CONTEXT.md`
  (en inglés; traducido al español en `06-glosario/content.md`).
- **Decisiones arquitectónicas:** `docs/Programa1/adr/0001-*.md` y
  `0002-*.md` (en inglés; traducidos al español en
  `05-decisiones-arquitectonicas/`).
- **Plan de implementación:** `docs/Programa1/implementation_plan.md`.
- **Sesión de grilling que originó todo esto:**
  `docs/Programa1/grilling-session-2026-05-26.md`.

Si CONTEXT.md o un ADR se actualiza en inglés, la traducción aquí debe
re-sincronizarse antes de entregar.
