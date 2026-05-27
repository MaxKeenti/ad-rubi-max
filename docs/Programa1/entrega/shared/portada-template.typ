// === Template function definition ===
#let portada(
  titulo_carrera,
  titulo_materia,
  titulo_practica,
  titulo_secuencia,
  titulo_alumno,
  titulo_profesorx,
  titulo_fecha,
  carrera,
  materia,
  practica,
  secuencia,
  alumnos,
  profesorx,
  fecha,
) = [
  #set text(font: "ITC Avant Garde Gothic")

  #set page(
    paper: "us-letter",
    margin: (left: 3cm, top: 2.5cm, right: 2.5cm, bottom: 2.5cm)
  )
  #set align(center)
  
  // === Logos ===
  #let logo-ipn = image("media/logos/IPN_Logo.svg", height: 3.5cm)
  #let logo-upiicsa = image("media/logos/UPIICSA_Logo.svg", height: 3.5cm)

  // === Institutos ===
  #let ipn = "INSTITUTO POLITÉCNICO NACIONAL"
  #let upiicsa = "UNIDAD PROFESIONAL INTERDISCIPLINARIA DE INGENIERÍA Y CIENCIAS SOCIALES Y ADMINISTRATIVAS"

  // === Header with logos ===
  #grid(
    columns: (1fr, 1fr),
    [#align(left)[#logo-ipn]],
    [#align(right)[#logo-upiicsa]],
  )

  // === Titles ===
  #v(1.25cm)
  #text(18pt, weight: "semibold")[#ipn]
  #v(0.4cm)
  #text(17pt, weight: "semibold")[#upiicsa]
  
  // Add some space before the columns start
  #v(1cm) 

  // === Two Column Grid ===
  #grid(
    columns: (1fr, 1fr), // Two equal columns
    row-gutter: 0.8cm,   // Space between rows
    column-gutter: 0.5cm,// Space between columns
    
    // --- Row 1 ---
    [
      #text(11pt, stretch: 75%)[#titulo_carrera]
      #v(0.1cm)
      #text(12pt, weight: "semibold")[#carrera]
    ],
    [
      #text(11pt, stretch: 75%)[#titulo_materia]
      #v(0.1cm)
      #text(12pt, weight: "semibold")[#materia]
    ],

    // --- Row 2 ---
    [
      #text(11pt, stretch: 75%)[#titulo_practica]
      #v(0.1cm)
      #text(12pt, weight: "semibold")[#practica]
    ],
    [
      #text(11pt, stretch: 75%)[#titulo_secuencia]
      #v(0.1cm)
      #text(12pt, weight: "semibold")[#secuencia]
    ],

    // --- Row 3 ---
    [
       // We keep the list logic here, but now it lives in the left column
       #text(11pt, stretch: 75%)[#titulo_alumno]
       #v(0.1cm)
       #text(12pt, weight: "semibold")[
        #if type(alumnos) == array {
          // We align left strictly for the bullets so they look nice
          align(center, list(..alumnos.sorted())) 
        } else {
          alumnos
        }
      ]
    ],
    [
      #text(11pt, stretch: 75%)[#titulo_profesorx]
      #v(0.1cm)
      #text(12pt, weight: "semibold")[#profesorx]
    ],

    // --- Row 4 ---
    // Since we have an odd number of items, Fecha takes the last spot on the left.
    // If you want it centered at the bottom, we would move it out of the grid.
    [
      #text(11pt, stretch: 75%)[#titulo_fecha]
      #v(0.1cm)
      #text(12pt, weight: "semibold")[#fecha]
    ]
  )
]