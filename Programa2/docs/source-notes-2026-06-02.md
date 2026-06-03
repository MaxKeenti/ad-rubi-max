# Source Notes - 2026-06-02

Source material for Programa2 planning. This document transcribes and
normalizes the two attached classroom-note images. Unclear handwritten words
are marked with `[unclear]`.

---

## Attached Image 1

Approximate transcription:

```text
llamar casos con prompt negativos es decir, no debe de
tener nada que ver con matar.

Cada peticion se debe condicionar con casos negativos.

Se debe de suscribirse a alguno de estos:

- Google Gemini API (mas nueva)
- OpenRouter (mas basica pero muy rudimentaria)
- Groq [unclear]
- Hugging Face.

Arquitectura de una App con IA

App Android -> HTTP -> API IA -> Json -> UI Compose

Para el lunes 8

La 3ra es entre el lunes 15 y viernes 19
```

## Attached Image 2

The image is rotated. Approximate transcription:

```text
- Multiplataforma
- PWA
- Backend as a Service
- Offline-First

Caso practico 2

En la epoca actual es comun encontrar en las escuelas un
alto numero de reprobacion, lo que implica un deficit alto
en el nivel educativo de la poblacion.

Estudios han demostrado que el factor animico es crucial en
el desempeno del estudiante, [unclear; likely complementado]
con palabras de aliento, lo anterior puede ser una diferencia.

Realizar una aplicacion de chat que permita al usuario
explicar sus miedos e inquietudes y reciba una retroalimentacion
motivacional.
```

## Derived Requirements

- Build an Android chat app for student fears, worries, and academic stress.
- The response must be motivational feedback, not general entertainment.
- The app must call an AI API over HTTP and receive JSON for Compose to render.
- Each AI request must be conditioned with negative cases.
- Safety boundary: the output must not provide content related to killing,
  murder, self-harm methods, or harm instructions.
- Subscribe to or configure one AI provider from the listed options.
- First planning/checkpoint target from the notes: Monday 2026-06-08.
- Next/third delivery window from the notes: Monday 2026-06-15 to Friday
  2026-06-19.

## External References Checked

These are not classroom requirements; they are implementation and safety
references checked while turning the notes into a practical plan.

- Google Gemini text generation docs:
  https://ai.google.dev/gemini-api/docs/text-generation
- Google Gemini structured output docs:
  https://ai.google.dev/gemini-api/docs/structured-output
- Google Gemini API key docs:
  https://ai.google.dev/gemini-api/docs/api-key
- Mexico Linea de la Vida, 800 911 2000:
  https://www.gob.mx/conasama/articulos/linea-de-la-vida-800-911-2000?idiom=es
- SAMHSA 988 Suicide & Crisis Lifeline:
  https://www.samhsa.gov/mental-health/988

## Open Questions

- Whether the professor expects a persistent chat history or only a single
  in-memory demo conversation.
- Whether a Backend as a Service requirement applies to this case practical or
  was only a course-topic list in the notes.
- Whether the app must support a PWA/multiplatform variant or remain Android
  only.
- Whether screenshots/formal Typst deliverables should be created immediately
  or after the app scaffold exists.
