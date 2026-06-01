---
name: explicar-tarea
description: Explica en español, de forma clara y detallada, lo que dice una tarea de docs/tasks/. Dado un ID de tarea (ej. "03", "03-melanie-login"), produce una explicación narrativa en español pensada para Melanie (que no lee inglés con fluidez) — cubre qué hace la tarea, por qué, qué archivos crea, cómo verificar que quedó bien, y los detalles que es fácil equivocar. Usar cuando el usuario pida "explica la tarea X", "qué hace la tarea X", "/explicar-tarea X", o invoque la skill por nombre.
---

# explicar-tarea — explicación en español de tareas

Las tareas en `docs/tasks/` están escritas en inglés porque
toda la cadena de decisión del proyecto (CONTEXT.md, ADRs, plan,
grilling logs) está en inglés y consolidar ahí evita drift. Pero Melanie
no lee inglés con fluidez y necesita entender qué va a construir antes
de meter manos al código.

Esta skill toma una tarea, la lee completa, y produce una **explicación
narrativa en español** — no una traducción literal, sino una conversación
que un compañero técnico tendría con Melanie para asegurarse de que
entiende qué va a hacer.

## Cómo se invoca

```
/explicar-tarea 03
/explicar-tarea 03-melanie-login
/explicar-tarea docs/tasks/03-melanie-login.md
```

Resolución del ID: igual que `/task-run` — número, slug parcial, o ruta
explícita. Si hay ambigüedad, listar las opciones y preguntar cuál.

## Cómo estructurar la explicación

La salida tiene **cinco secciones** en este orden. No omitir ninguna.

### 1. ¿Qué es esta tarea, en una frase?

Una oración en español, lo que la tarea construye y para quién. Ejemplo:

> Esta tarea construye la pantalla de Login y su ViewModel — la pantalla
> con la que el operador entra a la app.

### 2. ¿Por qué importa?

Dos o tres oraciones explicando cómo encaja esta tarea con el resto del
proyecto. Citar el documento de entrega correspondiente si ayuda
(ej. "Esto cumple el requerimiento RF-AUTH-01 del documento 02").

### 3. ¿Qué vas a crear o modificar?

Lista en español de los archivos, con una frase corta por cada uno de
qué responsabilidad tiene. Mantener los nombres de archivo y nombres
de clase en inglés — son código, no traducir.

Ejemplo:

> - `ui/auth/LoginScreen.kt` — la pantalla en Compose. Tiene dos
>   campos (correo y contraseña), un botón "Iniciar Sesión", y muestra
>   errores debajo si las credenciales fallan.
> - `ui/auth/LoginViewModel.kt` — el cerebro detrás de la pantalla.
>   Mantiene el estado actual (qué texto hay en los campos, si está
>   cargando), valida, y llama a `AuthRepository.signIn(...)` cuando el
>   usuario pulsa el botón.

### 4. ¿Cómo sabes que quedó bien? (Criterios de aceptación)

Lista de los criterios de aceptación traducidos al español, en forma de
checklist. Mantener referencias a CP-XX como están — son códigos de
caso de prueba.

Ejemplo:

> - [ ] La app compila sin errores (`./gradlew assembleDebug` termina en `BUILD SUCCESSFUL`).
> - [ ] Cuando abres la app, ves la pantalla de Login.
> - [ ] Si pulsas "Iniciar Sesión" con los campos vacíos, el botón debería
>   estar deshabilitado o mostrar un error.
> - [ ] Caso de prueba CP-01 (flujo de autenticación) pasa parcialmente
>   contra el repo Fake — ver `docs/entrega/08-plan-de-pruebas/content.md`.

### 5. Detalles que es fácil equivocar

La sección más valiosa para Melanie. Traducir los "Pitfalls / notes" del
inglés, pero **expandirlos**: si la nota dice "Hilt + Compose + kapt
combo bites", en español elabora *por qué* es problemático y cómo se ve
el error típico. La meta es que cuando Melanie tope con el error,
reconozca "ah, este es el problema que me advirtió la explicación."

Ejemplo:

> **Cuidado con el "Unresolved reference: HiltAndroidApp".** Es el error
> más típico de bootstrap con Hilt. Significa que el procesador kapt
> (que genera las clases que Hilt necesita) no está corriendo. Para
> arreglarlo, verifica que en `app/build.gradle.kts` esté:
>
> 1. `plugins { id("org.jetbrains.kotlin.kapt") }` aplicado.
> 2. `kapt("com.google.dagger:hilt-compiler:2.52")` en dependencies.
> 3. Que estás compilando en modo Debug, no Release (Hilt-compiler corre
>    en cada compile pero a veces Release falla por settings de R8).
>
> Si después de eso sigue, limpia el build: `./gradlew clean` y vuelve a
> probar.

## Tono

- **Compañero técnico que enseña**, no profesor ni manual seco.
- **Tutea** (es México, "tú" no "usted").
- **Concreta sobre lo abstracto** — en vez de "esta arquitectura aplica
  el patrón Repository", di "el código habla con `AuthRepository`, que
  es una interfaz que Max ya escribió, así no necesitas saber qué está
  pasando con Firebase directamente."
- **Reconocer cuando algo es difícil** — si la tarea es la pantalla más
  complicada (06 AddEdit Purchase, 14 PurchaseRepository real), decirlo:
  "Esta es de las tareas más densas del proyecto, no te preocupes si
  tarda más de lo estimado."
- **Si la tarea cita un ADR o un término del glosario**, dar la idea
  básica en una oración. No mandar a Melanie a leer 5 documentos antes
  de poder empezar.

## Qué traducir vs. qué dejar en inglés

| Traducir al español | Dejar en inglés |
|---|---|
| Prosa, explicaciones, advertencias | Nombres de archivo (`LoginScreen.kt`) |
| Etiquetas de UI que verá el usuario final ("Iniciar Sesión") | Nombres de clase (`AuthRepository`) |
| Conceptos del dominio que tienen palabra en español | Nombres de campo (`pricePerTonCentavos`, `dateKey`) |
| Pasos de verificación | Términos técnicos sin traducción establecida (Compose, Hilt, Firestore, ViewModel) |
| Mensajes de error humanos | Nombres de funciones, métodos, propiedades |

Regla simple: **si lo va a tipear, lo dejas en inglés. Si lo va a leer
para entender, lo traduces.**

## Qué NO hacer

- **No reproducir la tarea palabra por palabra en español.** Es una
  explicación, no una traducción. Reformula. Conecta. Agrega contexto
  cuando ayuda.
- **No esconder la información técnica.** Si la tarea menciona
  `FieldValue.serverTimestamp()`, mencionalo también — Melanie lo verá
  en el código. Solo explica qué es ("es un valor especial que Firestore
  reemplaza con la hora del servidor cuando guarda").
- **No editar archivos.** Esta skill solo lee la tarea y produce una
  explicación de texto en el chat. Si después Melanie quiere ejecutar la
  tarea, eso es `/task-run`.
- **No traducir las tareas físicamente al disco.** Si lo hicieras, las
  dos versiones empezarían a drift. La fuente única son los archivos en
  inglés; esta skill produce la lectura en español on-demand.

## Si la tarea es ambigua

Si al leer la tarea no entiendes algo lo suficientemente bien para
explicarlo, **no inventes**. Di en la explicación:

> Hay una parte de esta tarea que no me queda 100% clara: <X>. Antes de
> empezar, pregúntale a Max o revisa <documento referenciado>.

Mejor honesto que confidente y equivocado.
