# Task 20 — Max — Fix soft-delete refresh and offline sync regressions

**Status:** done in code on 2026-05-31; final test results are summarized
in `../guia-pruebas-melanie.md`.

**Owner:** Max
**Estimated effort:** ~3-4 hours (1h diagnóstico/repro + 1.5h fix + 1h re-run CP-06/CP-07 con Melanie)
**Prerequisites:** `14-max-purchase-repository-real`
**Day:** post-entrega-v1 / hotfix antes de imprimir el reporte final

## Goal

Cerrar los dos CPs que Melanie marcó como **Falla** en
`docs/guia-pruebas-melanie.md` (corrida 30-31/05/2026, Xiaomi
15T, Android 16):

- **CP-06 — Soft-delete:** la compra eliminada **no desaparece** del
  Historial pestaña "Todos" hasta que el operador cambia entre chips
  de proveedor. Firestore sí queda con `deletedAt` poblado, y "Últimas
  5 compras" del Dashboard sí se actualiza. El bug es de **refresco de
  UI local**, no de persistencia.

- **CP-07 — Captura offline:** al guardar en modo avión el botón se
  queda en "Guardando..." indefinidamente; el usuario tiene que salir
  con la flecha atrás. La compra sí aparece en "Últimas 5" con badge
  pendiente, pero al recuperar red el badge **no se quita nunca** — el
  doc aparenta no sincronizar.

Sin estos dos fixes, los CPs quedan en rojo en
`08-plan-de-pruebas/content.md` y el entregable no se puede defender.

## Inputs (leer antes de tocar)

- `docs/guia-pruebas-melanie.md` — secciones CP-06 (líneas
  261-287) y CP-07 (líneas 289-323) con la observación exacta de
  Melanie y la **Hoja de resultados** (líneas 501-522).
- `docs/tasks/14-max-purchase-repository-real.md` —
  decisiones de diseño del repo (denormalización, ventana 24h,
  `serverWrittenAt`, `dateKey`). El fix no debe romper CP-02, CP-03,
  CP-04, CP-05, CP-11.
- `docs/entrega/04-modelo-de-datos/content.md` § 2.3 —
  semántica de `deletedAt`/`deletedBy` y los tres timestamps.
- `docs/adr/0002-server-side-authz.md` — el reloj de
  auditoría vive en `serverWrittenAt`. **No** estamos cambiando ese
  reloj; el cambio aquí es sobre `deletedAt`.
- `app/src/main/java/com/example/mangos/data/repository/firestore/PurchaseRepositoryFirestoreImpl.kt` —
  los queries reactivos y el `softDelete`/`add`.
- `app/src/main/java/com/example/mangos/ui/purchases/AddEditPurchaseViewModel.kt` —
  flujo de guardado, `isSaving`/`saveCompleted`.
- `app/src/main/java/com/example/mangos/ui/dashboard/DashboardViewModel.kt` —
  consumidor de `observeRecentWithPending`, donde vive el badge.
- `firestore.rules` — verificar que el cambio en la forma de
  `deletedAt` (de `serverTimestamp` a `Timestamp.now()`) no rompa
  ninguna validación (ver pitfall abajo).

## Diagnóstico (hipótesis a confirmar antes de tocar código)

### CP-06 — pending-write keeps deleted doc visible

`PurchaseRepositoryFirestoreImpl.softDelete()`:

```kotlin
val updates: Map<String, Any> = mapOf(
    "deletedAt" to FieldValue.serverTimestamp(),
    "deletedBy" to deletedBy,
)
```

Y los listados activos:

```kotlin
firestore.collection(PURCHASES)
    .whereEqualTo("deletedAt", null)         // ← AQUI
    .orderBy("enteredAt", Query.Direction.DESCENDING)
    ...
    .snapshots()                              // default = MetadataChanges.EXCLUDE
```

Mientras la escritura está en cola local, el sentinel
`serverTimestamp()` se proyecta como `null` en el cache → el doc
**sigue matcheando** `whereEqualTo("deletedAt", null)` → aparece en la
lista. Cuando el server resuelve el timestamp, el valor pasa a ser un
`Timestamp` real y el filtro lo excluye — pero ese cambio puede no
emitirse en la ventana visible si el listener no re-evalúa
agresivamente, o si la app fue a background entre medio. Melanie ve
que solo se cae el doc cuando ella reinicia el flow al cambiar de chip
de proveedor (lo que sí dispara un `flatMapLatest` con nuevo listener
en `PurchaseHistoryViewModel.purchases`, línea 48).

**Validar la hipótesis** antes del fix: agregar log a
`observeRecent`/`observeBySupplier` que imprima
`snap.metadata.hasPendingWrites` y `snap.documents.size` cada emisión.
Reproducir el delete y observar:

1. Emit con `hasPendingWrites = true`, doc todavía dentro.
2. Emit con `hasPendingWrites = false`, doc fuera (si llega).

Si nunca llega (2) sin un reinicio de listener, la hipótesis queda
confirmada.

### CP-07 — `.set().await()` nunca resuelve offline

`PurchaseRepositoryFirestoreImpl.add()` termina con:

```kotlin
collection.document(newId).set(data).await()
newId
```

Per los docs del SDK de Firestore Android: el `Task` devuelto por
`set()`/`update()` **resuelve cuando el server confirma**, no cuando
el cache local persiste. Offline esa `Task` queda pendiente
indefinidamente. La corrutina de `AddEditPurchaseViewModel.onSave` se
queda colgada en el `await`, `formState.isSaving` nunca pasa a
`false`, `saveCompleted` nunca se prende, y el botón se queda en
"Guardando..." (`AddEditPurchaseScreen.kt:250`).

Adicional — `observeRecentWithPending`:

```kotlin
.snapshots()           // = MetadataChanges.EXCLUDE por default
.map { snap ->
    snap.documents.mapNotNull { doc ->
        PurchaseRepository.PendingAware(
            purchase = ...,
            isPending = doc.metadata.hasPendingWrites(),
        )
    }
}
```

Cuando el server confirma una escritura pendiente, el único cambio en
el doc es `metadata.hasPendingWrites: true → false`. Con
`MetadataChanges.EXCLUDE` ese evento **no se emite** — el `Flow` se
queda colgado en la última emisión donde `isPending = true`. Por eso
Melanie ve el badge "pendiente" para siempre aunque la escritura sí
haya sincronizado (lo que es verificable en Firebase Console).

**Validar la hipótesis** antes del fix:

1. Capturar offline, esperar a que el botón se quede en "Guardando…".
2. Sin tocar nada, salir con la flecha atrás (la corrutina sigue viva
   en `viewModelScope` mientras la VM exista).
3. Apagar modo avión.
4. Abrir Firebase Console → `purchases` → buscar el doc nuevo. Si
   está presente con `serverWrittenAt` poblado, el doc **sí
   sincronizó** y el bug es puramente de UI (`MetadataChanges.EXCLUDE`).
5. Si el doc **no** está en Firestore, hay un problema adicional de
   propagación de auth/queue que hay que diagnosticar aparte (token
   de Auth expirado offline, etc.).

## Outputs

### Fix CP-06 — `Timestamp.now()` para `deletedAt`

**`app/src/main/java/com/example/mangos/data/repository/firestore/PurchaseRepositoryFirestoreImpl.kt`:**

```kotlin
override suspend fun softDelete(id: String, deletedBy: String): Result<Unit> = runCatching {
    val updates: Map<String, Any> = mapOf(
        "deletedAt" to Timestamp.now(),         // client clock — local cache lo ve no-null inmediato
        "deletedBy" to deletedBy,
    )
    firestore.collection(PURCHASES)
        .document(id)
        .update(updates)
        .await()
    Unit
}
```

Justificación: `deletedAt` no es un reloj de autorización (eso vive en
`serverWrittenAt` por ADR-0002). Su uso es **(a) ocultar de la UI** y
**(b) auditoría aproximada**. Un skew de cliente de <60s es aceptable
para ambos. El beneficio: el cache local ve un timestamp no-null al
instante → `whereEqualTo("deletedAt", null)` lo excluye en el mismo
tick → la lista se refresca sin esperar al server.

**Opcional (decidir durante implementación):** agregar
`serverDeletedAt = FieldValue.serverTimestamp()` como campo paralelo
para conservar el reloj autoritativo. Si se agrega, actualizar
`04-modelo-de-datos/content.md` y `firestore.rules` (ver pitfalls).

### Fix CP-07 (parte A) — no bloquear el save en `await()`

**`app/src/main/java/com/example/mangos/data/repository/firestore/PurchaseRepositoryFirestoreImpl.kt`:**

Cambiar `add()` y `update()` para no bloquear en el ack del server.
Dos estrategias aceptables — elegir una y dejar nota en el commit:

**Opción 1 (recomendada): fire-and-forget con error-listener.**

```kotlin
override suspend fun add(purchase: Purchase): Result<String> = runCatching {
    // ... mismo build del Map data ...
    val docRef = collection.document(newId)
    docRef.set(data)                                       // sin .await()
        .addOnFailureListener { ex ->
            Log.e(TAG, "set() falló para purchase=$newId", ex)
        }
    newId
}
```

El cache local persiste la escritura sincronamente, `add()` devuelve
de inmediato, la VM transita a `saveCompleted = true`, la pantalla se
cierra. Si el server después rechaza la escritura (rules, etc.) el
listener loggea y el snapshot listener emitirá el rollback.

**Opción 2 (defensiva): timeout corto.**

```kotlin
val ackTask = docRef.set(data)
withTimeoutOrNull(1_500.milliseconds) { ackTask.await() }
// Si tardó >1.5s, asumimos offline-queued; el Task sigue vivo y
// resolverá cuando vuelva la red. Devolvemos newId igual.
newId
```

Más conservador: si hay red y el server ack es <1.5s, espera; si no,
sigue. Esta opción es preferible si quieres mantener feedback de
errores síncronos cuando la red sí está.

**No tocar** `softDelete()` ni `update()` en su versión de await por
ahora — los swipes y ediciones desde el historial son operaciones
menos críticas. Si quieres uniformidad, aplica la misma estrategia a
los tres.

### Fix CP-07 (parte B) — incluir metadata changes en el flow del badge

**`app/src/main/java/com/example/mangos/data/repository/firestore/PurchaseRepositoryFirestoreImpl.kt`:**

```kotlin
import com.google.firebase.firestore.MetadataChanges

override fun observeRecentWithPending(
    limit: Int,
): Flow<List<PurchaseRepository.PendingAware>> =
    firestore.collection(PURCHASES)
        .whereEqualTo("deletedAt", null)
        .orderBy("enteredAt", Query.Direction.DESCENDING)
        .limit(limit.toLong())
        .snapshots(MetadataChanges.INCLUDE)              // ← cambio clave
        .map { snap ->
            snap.documents.mapNotNull { doc ->
                val purchase = doc.toPurchase() ?: return@mapNotNull null
                PurchaseRepository.PendingAware(
                    purchase = purchase,
                    isPending = doc.metadata.hasPendingWrites(),
                )
            }
        }
```

Solo el query del badge necesita `MetadataChanges.INCLUDE` — los
demás (`observeRecent`, `observeBySupplier`, `observeByDateKey`,
`observeByDateRange`) no consumen `hasPendingWrites`, y agregar
metadata events innecesarios sube el churn de recomposición sin
beneficio.

### Verificar `FakePurchaseRepository`

`app/src/main/java/com/example/mangos/data/repository/fake/FakePurchaseRepository.kt`
no cambia — la interfaz `PurchaseRepository` no se toca. Solo asegúrate
de que sigue compilando después del cambio.

## Acceptance criteria

- [ ] **CP-06 pasa de punta a punta** en la app real:
  - Admin elimina una compra desde Compras → Todos. La compra
    desaparece de la lista **inmediatamente** (no requiere cambio de
    chip ni reinicio de pantalla).
  - La compra tampoco aparece al cambiar entre pestañas
    `Inicio / Compras / Proveedores / Reportes` y volver.
  - El doc en Firestore tiene `deletedAt` poblado (Timestamp) y
    `deletedBy` con el uid del admin.
- [ ] **CP-07 pasa de punta a punta**:
  - Operador en modo avión captura una compra. El botón Guardar **no
    se queda colgado** — la pantalla cierra en <2s y vuelve al
    Dashboard.
  - La compra aparece en "Últimas 5" con badge "pendiente".
  - Al desactivar modo avión y esperar 10-30s, el badge "pendiente"
    desaparece (porque ahora `MetadataChanges.INCLUDE` emite el flip).
  - El doc en Firestore tiene `serverWrittenAt` poblado (no null).
- [ ] **CPs previamente verdes no se rompen**:
  - CP-02 (captura normal con red) sigue cerrando la pantalla y
    apareciendo en la lista.
  - CP-03 (sin precio), CP-04 (UNREGISTERED), CP-05 (ventana 24h),
    CP-11 (dateKey) siguen pasando.
  - Tests de reglas (`tests/rules/rules.test.js`) siguen verdes; después
    de task 19 son 26/26. El shape del documento `purchases` no cambió (solo el
    valor literal de `deletedAt` pasa de sentinel server a
    `Timestamp` cliente, ambos son `timestamp` desde la óptica de las
    rules).
- [ ] **Actualizar la hoja de resultados** en
  `docs/guia-pruebas-melanie.md` (CP-06 y CP-07 a Pasa con
  link al PR del fix) **solo después** de re-correr con Melanie en el
  mismo Xiaomi 15T.
- [ ] **Actualizar `docs/entrega/08-plan-de-pruebas/content.md`**
  si hay alguna nota que asuma el bug.

## Pitfalls / notes

- **Reglas Firestore sobre `deletedAt`**: si `firestore.rules` valida
  que `deletedAt` sea `request.time` o un sentinel server, el cambio a
  `Timestamp.now()` (cliente) lo rompe. Grep `deletedAt` en
  `firestore.rules` antes de hacer el cambio. Si hay validación,
  ablandarla a `request.resource.data.deletedAt is timestamp` — eso
  basta porque la rule ya bloquea escrituras no autorizadas por `uid`.
- **Tests de emulador**: si algún test de `tests/rules/rules.test.js`
  escribe `deletedAt` con `serverTimestamp()`, hay que armonizar — usar
  `Timestamp.now()` en el test o aceptar ambas formas en la rule.
- **`MetadataChanges.INCLUDE` aumenta el churn**: cada metadata flip
  re-emite la lista entera. En el Dashboard son 5 items, está bien. No
  propagar esto a los queries grandes (historial completo, reportes)
  sin razón.
- **`Timestamp.now()` y el cache offline**: localmente el cache acepta
  el valor inmediato. Cuando la red vuelve, Firestore sincroniza el
  valor cliente al server tal cual — `Timestamp.now()` se preserva, no
  se reemplaza por el reloj del server. Esto es deliberado: el reloj
  autoritativo de auditoría sigue siendo `serverWrittenAt`; `deletedAt`
  es informativo. Documentarlo en
  `entrega/04-modelo-de-datos/content.md` § `purchases` con una línea
  ("`deletedAt` es client-side timestamp; el reloj autoritativo es
  `serverWrittenAt` por ADR-0002").
- **Verificación del bug B (badge pendiente persistente)** requiere
  que el doc **sí haya sincronizado** una vez recuperada la red. Si
  Firebase Console muestra que el doc no llegó al server después de
  varios minutos, hay un problema separado (cola offline corrupta,
  token de auth expirado, regla rechazando el write) que este task
  **no** cubre — abrir issue separado.
- **No regresar a `serverTimestamp()` para `deletedAt`** durante un
  debug rápido aunque parezca "más correcto" — el bug CP-06 reaparece.
- **Avisar a Melanie cuando esté listo el APK con los fixes** para
  agendar la re-corrida de CP-06 y CP-07. El resto de los CPs no
  necesita rerun (los demás ya están verdes en su hoja).

## Verificación (a llenar cuando se ejecute el task)

```
Fecha de ejecución: ____________
APK probado: mangos-vX.Y.apk (SHA: ____________)
Re-corrida CP-06 con Melanie: [ ] Pasa  [ ] Falla — notas: __________
Re-corrida CP-07 con Melanie: [ ] Pasa  [ ] Falla — notas: __________
Tests de emulador: tests __ / pass __ / fail __
```
