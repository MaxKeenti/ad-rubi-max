// Shared style preamble for all entrega documents.
// Import with:  #import "../shared/style.typ": *
//
// Mirrors the conventions in docs---ingPruebas/docs/typst-style.md.

#import "@preview/codly:1.3.0": *
#import "@preview/cmarker:0.1.6" as cmarker

#let setup-doc(body) = {
  show raw: set text(
    font: "JetBrainsMono NFM",
    weight: "medium",
    size: 0.9em,
  )

  show: codly-init.with()

  codly(
    languages: (
      kotlin: (name: "Kotlin", icon: "", color: rgb("#7F52FF")),
      kt: (name: "Kotlin", icon: "", color: rgb("#7F52FF")),
      typ: (name: "Typst", icon: "", color: rgb("#239DAD")),
      json: (name: "JSON", icon: "", color: rgb("#888888")),
      js: (name: "JS Rules", icon: "", color: rgb("#F7DF1E")),
    ),
    number-format: n => str(n),
  )

  set text(
    font: "ITC Avant Garde Gothic",
    lang: "es",
    weight: "semibold",
    size: 11pt,
  )

  set page(
    paper: "us-letter",
    margin: (left: 3cm, top: 2.5cm, right: 2.5cm, bottom: 2.5cm),
    numbering: "1",
  )

  set par(justify: true, leading: 1.4em)
  set heading(numbering: "1.")
  set list(indent: 1.5em)

  body
}

// Canonical alumnos array — single source of truth for the team roster.
#let integrantes = (
  "González Calzada Maximiliano — 2021601769",
  "Sosa Montoya Melanie Rubí — 2024601345",
)

// Canonical portada call. Each main.typ passes its own `practica` title.
#let portada-mangos(practica-title) = {
  import "portada-template.typ": portada
  portada(
    "UNIDAD DE APRENDIZAJE",
    "PROYECTO FINAL",
    "SECUENCIA Y PERIODO",
    "BOLETAS",
    "ALUMNOS",
    "PROFESOR",
    "FECHA",
    "Programación Móvil",
    practica-title,
    "6NM61 2026-2",
    "2021601769 · 2024601345",
    integrantes,
    "Huerta Valdepeña Erick",
    "Junio de 2026",
  )
}

// NOTE: do not define a render-md(path) helper here.
// Typst's read() resolves relative to the file that *contains* the call,
// so wrapping read() inside a helper in this file would look for
// shared/content.md (wrong). Each main.typ does its own read+render:
//
//   #import "/shared/style.typ": *
//   #show: setup-doc
//   #portada-mangos("Modelo de Datos")
//   #pagebreak()
//   #cmarker.render(read("content.md"))
