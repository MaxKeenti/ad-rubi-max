#import "/shared/style.typ": *
#show: setup-doc

#portada-mangos("Entrega Final")
#pagebreak()

#outline(title: "Indice")
#pagebreak()

#cmarker.render(read("01-resumen-ejecutivo/content.md"))
#pagebreak()

#cmarker.render(read("02-requerimientos/content.md"))
#pagebreak()

#cmarker.render(read("03-arquitectura/content.md"))
#pagebreak()

#cmarker.render(read("04-modelo-de-datos/content.md"))
#pagebreak()

#cmarker.render(read("05-decisiones-arquitectonicas/content.md"))
#pagebreak()

#cmarker.render(read("06-glosario/content.md"))
#pagebreak()

#cmarker.render(read("07-manual-de-usuario/content.md"))
#pagebreak()

#cmarker.render(read("08-plan-de-pruebas/content.md"))
#pagebreak()

#cmarker.render(read("09-conclusiones/content.md"))
