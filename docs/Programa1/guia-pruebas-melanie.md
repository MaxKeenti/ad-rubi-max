# Guía de pruebas manuales — Melanie

> **Para quién es esto:** Melanie.
> **Quién lo escribió:** Max.
> **Para qué:** correr los 16 casos de prueba (CP-01 a CP-16) del plan de
> pruebas antes de la entrega final del 1 de junio. Esta guía es la
> versión "ejecutable" de `docs/Programa1/entrega/08-plan-de-pruebas/content.md`
> — el documento de allá es el formal que se imprime; éste es el que tú
> usas sentada con el teléfono en la mano.

## 1. Antes de empezar

### 1.1 Lo que vas a necesitar

- [ ] **Un teléfono Android** con Android 16 (API 36) o superior.
  Si no tienes uno físico, abre el emulador de Android Studio con un
  AVD Pixel-cualquiera, API 36.
- [ ] **El APK firmado**, archivo `app-debug.apk`. Lo puedes encontrar en `Programa1/app/build/outputs/apk/debug/app-debug.apk`.
- [ ] **Tus credenciales de operador**: te las paso por mensaje
  privado (no las pongas en commits). Si no te llegaron, escríbeme.
- [ ] **Las credenciales de admin**: también por mensaje privado.
- [ ] **Cloud Functions desplegadas** para el proyecto `mangos-372bd`.
  Los CP-13 a CP-15 usan la pestaña Usuarios; si al abrirla aparece un
  error de Functions o de permisos, detente y avísame.
- [ ] **`FIREBASE_WEB_API_KEY` configurada en Functions.** Sin esta
  variable, las confirmaciones de contraseña para crear Admin/promover
  Operador fallan aunque la contraseña sea correcta.
- [ ] **Acceso a la Consola de Firebase del proyecto `mangos-372bd`**
  con tu cuenta Google — Max te invita como editor antes de que
  arranques. Necesitarás entrar a Firestore unas 3 o 4 veces para
  confirmar que los datos quedaron bien escritos.
- [ ] **Un segundo dispositivo / emulador** — opcional pero útil para
  CP-05 (necesitas dos operadores distintos sesionados al mismo
  tiempo). Si solo tienes uno, puedes cerrar sesión y entrar con la
  otra cuenta entre pasos.

### 1.2 Cómo registrar resultados

Cada caso tiene:

- Una breve descripción de **qué prueba**.
- Una sección de **pasos** en tabla — igual al formato que ya
  conoces de `entrega/08-plan-de-pruebas`.
- Un **resultado esperado**.
- Dos casillas: `[ ] Pasa` / `[ ] Falla`.
- Un campo de **notas** para que escribas lo que viste, capturas, IDs
  de documentos, lo que sea relevante.

Al final del documento hay una **Hoja de resultados** con los 16 casos
en tabla. Cuando termines, llénala y mándamela (commit + push o
captura de la tabla por Slack — lo que prefieras).

### 1.3 Convención de las cuentas que vas a usar

Para que el documento sea legible voy a referirme a las cuentas con
estos alias:

| Alias en esta guía | Cuenta real | Rol |
|---|---|---|
| **OP-MEL** | la tuya — `melanie@…` (te paso la contraseña) | operator |
| **OP-MAX** | la mía — `max@…` (te paso la contraseña) | operator |
| **ADMIN** | `admin@admin.com` (te paso la contraseña) | admin |
| **OP-TEMP** | cuenta que vas a crear en CP-13 | operator |
| **ADMIN-TEMP** | cuenta que vas a crear en CP-14 | admin |

Nada en el código depende de los correos exactos, así que si los nombres
cambian, no te asustes — lo que importa es el **rol** (operator vs
admin).

Para CP-13 a CP-15 usa correos de prueba únicos, por ejemplo
`melanie.op.temp+YYYYMMDDHHmm@...` y
`melanie.admin.temp+YYYYMMDDHHmm@...`. No promociones **OP-MEL** ni
**OP-MAX**; la promoción retira la cuenta de Operador.

## 2. Pre-vuelo: setup

### 2.1 Instalar el APK

1. Descarga `mangos-v1.0.apk` en el teléfono (o cópialo al emulador
   con `adb install mangos-v1.0.apk`).
2. Si Android te avisa "instalar de fuentes desconocidas", acepta.
3. Abre la app, debe aparecer la pantalla de login con el logo de
   Mangos.

> Si la app crashea al abrir o la pantalla de login no aparece,
> **detente aquí** y escríbeme. Algo está mal con el APK y no tiene
> sentido seguir con los CPs.

### 2.2 Verificar que el seed data está sembrado

Antes del CP-01 confirma que en Firestore existan estos documentos
(consola → Firestore → colecciones):

- [ ] `users/{tu-uid-mel}` con `role: "operator"`.
- [ ] `users/{uid-de-max}` con `role: "operator"`.
- [ ] `users/{uid-admin}` con `role: "admin"`.
- [ ] `suppliers/UNREGISTERED` con `name: "Proveedor no registrado"`
  e `isActive: true`.
- [ ] **Al menos 3 proveedores activos** distintos de UNREGISTERED.
  Lo esperado (ver `tasks/17-max-seed-data.md` § Verificación):
  - `suppliers/hernandez-y-hermanos` — Veracruz, Ataulfo, activo.
  - `suppliers/mangos-del-pacifico` — Nayarit, Manila, activo.
  - `suppliers/frutas-selectas-sa` — Oaxaca, Tommy Atkins,
    **inactivo** (lo necesitamos para CP-08).

Si falta alguno de éstos, **escríbeme antes de seguir** — yo agrego lo
que falte desde la consola. Si están todos, sigue con el CP-01.

## 3. Los 16 casos

---

### CP-01 — Flujo de autenticación

**Qué prueba:** que se pueda entrar y salir de la app con cuentas de
operador y admin, y que la barra inferior sea distinta según el rol.

**Pasos:**

| # | Acción | Resultado esperado |
|---|---|---|
| 1 | Abre la app por primera vez | Aparece la pantalla de Login (no hay opción de "Crear cuenta") |
| 2 | Captura las credenciales de **OP-MEL** y pulsa "Iniciar sesión" | Aterrizas en el Dashboard. La barra inferior muestra **Dashboard / Compras / Reportes** (sin Proveedores ni Usuarios) |
| 3 | Pulsa el menú overflow (⋮) arriba a la derecha y elige "Cerrar sesión" | Regresas a Login |
| 4 | Captura las credenciales de **ADMIN** y pulsa "Iniciar sesión" | Aterrizas en el Dashboard. La barra inferior ahora muestra **Dashboard / Compras / Proveedores / Usuarios / Reportes** (aparecen las pestañas administrativas) |
| 5 | Cierra sesión con el menú overflow | Regresas a Login |

**Resultado:** `[x] Pasa` `[ ] Falla`

**Notas:**

```
Todo aparece como debe, los proveedores con el nombre correcto y los estados correspondientes y se pueden añadir proveedores (no añadi aun ninguno)

```

---

### CP-02 — Captura de compra normal

**Qué prueba:** que un operador pueda capturar una compra completa
con proveedor del catálogo, toneladas, precio y fecha.

**Preparación:** sesionada como **OP-MEL**.

**Pasos:**

| # | Acción | Resultado esperado |
|---|---|---|
| 1 | En el Dashboard, pulsa el FAB **(+)** abajo a la derecha | Se abre la pantalla "Registrar entrada" (AddEditPurchase) |
| 2 | En el dropdown de proveedor, selecciona **Hernández y Hermanos** | El dropdown muestra "Hernández y Hermanos" |
| 3 | En toneladas escribe `5.5` | El campo acepta el decimal |
| 4 | En precio por tonelada escribe `12500` (MXN) | El campo acepta el número |
| 5 | Confirma que la fecha por defecto es la de hoy (no la cambies) | La fecha hoy se ve en el selector |
| 6 | Pulsa **Guardar** | Vuelves al Dashboard. La compra aparece en "Últimas 5 compras" con "Hernández y Hermanos – 5.5 t" |
| 7 | Abre Firestore Console → `purchases` → ordena por `enteredAt` desc → abre el documento más reciente | El doc tiene `dateKey = "{fecha-de-hoy}"` (formato `"YYYY-MM-DD"`), `pricePerTonCentavos = 1250000`, `enteredAt` y `serverWrittenAt` ambos con timestamps. `supplierName = "Hernández y Hermanos"` denormalizado. `createdBy` = tu uid |

> **Tip:** `12500` MXN se debe guardar como `1250000` centavos (×100).
> Si en Firestore ves `12500` literal en `pricePerTonCentavos`, el
> mapping de la app está mal — anótalo como falla.

**Resultado:** `[x] Pasa` `[ ] Falla`

**Notas:**

```
La compra se guarda correctamente.
```

---

### CP-03 — Captura sin precio

**Qué prueba:** que el precio sea opcional y que los reportes
manejen correctamente las compras sin precio.

**Preparación:** sesionada como **OP-MEL**.

**Pasos:**

| # | Acción | Resultado esperado |
|---|---|---|
| 1 | FAB (+) → captura una compra: proveedor "Mangos del Pacífico", 2 toneladas, **deja el precio en blanco**, fecha hoy | El botón Guardar está habilitado (el precio es opcional) |
| 2 | Pulsa Guardar | Vuelves al Dashboard, la compra aparece |
| 3 | Abre Firestore → busca el doc nuevo | `pricePerTonCentavos == null` (literal `null`, no `0`) |
| 4 | Ve a la pestaña **Reportes** | El "Total de toneladas hoy" incluye estas 2 toneladas. El "Gasto de hoy en MXN" **no** incluye esta compra. Debajo del número de gasto aparece "(1 entrada sin precio)" |

> **Si en `pricePerTonCentavos` ves `0` en lugar de `null`**, la app
> está poniendo cero cuando debería poner null — anótalo como falla.

**Resultado:** `[x] Pasa` `[ ] Falla`

**Notas:**

```
Todo funciona correctamente.

```

---

### CP-04 — Captura contra "Proveedor no registrado" (UNREGISTERED)

**Qué prueba:** que cuando llega un camión de un proveedor que no
está dado de alta, el operador pueda usar la opción de comodín y
capturar el nombre real en texto libre.

**Preparación:** sesionada como **OP-MEL**.

**Pasos:**

| # | Acción | Resultado esperado |
|---|---|---|
| 1 | FAB (+) → en el dropdown selecciona **"Proveedor no registrado"** | Aparece un nuevo campo de texto "Nombre del proveedor" |
| 2 | En ese campo escribe `Mangos de Veracruz S.A.` | El texto aparece como lo capturaste |
| 3 | Captura 3 toneladas, precio en blanco, fecha hoy | OK |
| 4 | Pulsa Guardar | Vuelves al Dashboard |
| 5 | Cierra sesión, entra como **ADMIN**, ve a **Reportes** o **Compras** | La compra que acabas de capturar aparece bajo "Proveedor no registrado" y la nota libre `Mangos de Veracruz S.A.` es visible |
| 6 | Abre el doc en Firestore | `supplierId = "UNREGISTERED"` y `supplierNoteFreeform = "Mangos de Veracruz S.A."` |

**Resultado:** `[x] Pasa` `[ ] Falla`

**Notas:**

```
Funcionamiento correcto

```

---

### CP-05 — Ventana de edición de 24h del operador

**Qué prueba:** que el operador pueda corregir su propia compra
dentro de las primeras 24 horas, pero **no** la de otro operador, ni
una propia que ya tenga más de 24h.

**Preparación:** necesitas dos sesiones de operador distintas. Si
solo tienes un dispositivo, alterna sesión entre **OP-MEL** y
**OP-MAX**.

> **Nota sobre el paso 4 (compra > 24h):** no podemos retroactivar el
> `serverWrittenAt` desde la app. La regla del lado del servidor que
> rechaza la edición pasado las 24h ya está cubierta por los **tests
> automatizados del emulador** (`tests/rules/rules.test.js` — caso
> `operator cannot edit own purchase after 24h`). Para esta corrida
> manual, **marca el paso 4 como ✅ "cubierto por tests
> automatizados"** y enfócate en los pasos 1-3 y 5. Si quieres
> hacerlo de verdad, tendrías que sembrar manualmente en Firestore
> una compra con `serverWrittenAt` de hace 25 horas — me avisas si
> quieres que te haga uno.

**Pasos:**

| # | Acción | Resultado esperado |
|---|---|---|
| 1 | Sesionada como **OP-MEL**, captura una compra cualquiera (proveedor "Mangos del Pacífico", 1 tonelada, sin precio, hoy) | OK |
| 2 | En el Historial (pestaña Compras), pulsa esa compra para editarla, cambia la cantidad a 1.5 t y guarda | Edición permitida; la compra ahora dice 1.5 t |
| 3 | Cierra sesión. Inicia sesión como **OP-MAX**. Ve al Historial y trata de pulsar la compra que **OP-MEL** acaba de capturar | Una de dos: (a) la app oculta el botón de editar para esa compra; o (b) la app deja entrar a editar pero al pulsar Guardar muestra un error de permisos. Cualquiera de las dos es válida |
| 4 | (Cubierto por tests automatizados — ver nota arriba) | n/a |
| 5 | Cierra sesión. Entra como **ADMIN**. Edita esa misma compra cambiando la cantidad a `2` | Edición permitida — el admin no tiene ventana |

**Resultado:** `[x] Pasa` `[ ] Falla`

**Notas:**

```
Funcionamiento correcto

```

---

### CP-06 — Soft-delete

**Qué prueba:** que el "borrado" en realidad sea un soft-delete —
la compra desaparece de la UI pero el documento queda en Firestore
con `deletedAt` poblado para auditoría.

**Preparación:** sesionada como **ADMIN**. Necesitas al menos una
compra existente (las que capturaste en CPs anteriores funcionan).

**Pasos:**

| # | Acción | Resultado esperado |
|---|---|---|
| 1 | Ve a la pestaña **Compras**. Escoge una compra y deslízala (swipe) a un lado | Aparece la opción de Eliminar |
| 2 | Confirma eliminar | La compra desaparece de la lista. El Dashboard también deja de mostrarla en "Últimas 5" si estaba ahí |
| 3 | En Firestore Console, busca el doc por su id | El documento **sigue existiendo** — no se eliminó físicamente. Ahora tiene `deletedAt: <timestamp reciente>` y `deletedBy: <tu uid de admin>` |
| 4 | Vuelve a la pestaña Compras en la app | La compra eliminada no aparece en ninguna lista (Dashboard, Historial, Reportes) |

**Resultado:** `[X] Pasa` `[ ] Falla` — **corrección aplicada el
2026-05-31 (task 20); re-corrida con Melanie pendiente**.

**Notas:**

```

```
Pero si desaparece de las "Últimas 5 compras" y se muestra como debe de ser en el Firebase

**Re-corrida (post-fix, pendiente):**

- **Causa raíz:** `softDelete()` escribía `deletedAt` con
  `FieldValue.serverTimestamp()`. Mientras la escritura estaba en cola
  local, el cache de Firestore proyectaba el sentinel como `null`, y el
  query `whereEqualTo("deletedAt", null)` seguía matcheando el doc. Solo
  al recargar el listener (cambiar de chip de proveedor) la lista se
  refrescaba.
- **Fix:** ahora `softDelete()` usa `Timestamp.now()` del cliente. El
  cache ve un timestamp no-null en el mismo tick → la lista excluye el
  doc inmediatamente. El reloj autoritativo de auditoría sigue siendo
  `serverWrittenAt` (ADR-0002); `deletedAt` es informativo. La regla de
  Firestore se ablandó a `request.resource.data.deletedAt is timestamp`
  para acompañar el cambio (uid sigue anclado).
- **Verificación en desarrollo:** `./gradlew assembleDebug` ✓; tests de
  reglas del emulador 24/24 ✓.
- **Pendiente:** re-correr este caso en el mismo Xiaomi 15T con Melanie.
  Esperado: la compra eliminada desaparece del Historial > "Todos"
  inmediatamente, sin necesidad de cambiar entre chips de proveedor ni
  reiniciar la pestaña.

---

### CP-07 — Captura offline

**Qué prueba:** que la app funcione sin internet — las capturas se
guardan localmente, muestran indicador "pendiente", y se sincronizan
solas cuando vuelve la red.

**Preparación:** sesionada como **OP-MEL**, con internet.

**Pasos:**

| # | Acción | Resultado esperado |
|---|---|---|
| 1 | Activa **modo avión** en el teléfono (o desactiva wifi + datos en el emulador) | Sin conectividad. La app sigue funcionando |
| 2 | Captura **3 compras seguidas** (proveedores y cantidades a tu elección — usa el catálogo, no UNREGISTERED) | Cada compra se guarda y vuelves al Dashboard sin error |
| 3 | Mira el Dashboard | Las 3 compras aparecen en "Últimas 5 compras" cada una con un indicador visual de **"pendiente"** (puede ser un icono pequeño o una etiqueta — lo importante es que se distingan visualmente de las sincronizadas) |
| 4 | Desactiva modo avión | La app recupera conectividad |
| 5 | Espera 10-30 segundos sin tocar nada | Los indicadores "pendiente" deben **desaparecer** uno por uno conforme se sincroniza cada compra |
| 6 | En Firestore Console busca las 3 compras | Las 3 existen, cada una con `serverWrittenAt` poblado (no null) |

> **Si los indicadores no aparecen mientras estás en modo avión**, o
> **si no desaparecen al recuperar red**, anótalo como falla y
> describe qué pasó (siguen pendientes para siempre, parpadean, etc.).

**Resultado:** `[X] Pasa` `[ ] Falla` — **corrección aplicada el
2026-05-31 (task 20); re-corrida con Melanie pendiente**.

**Notas:**

```

```

**Re-corrida (post-fix, pendiente):**

Eran dos bugs en uno; los dos quedaron corregidos en el task 20.

- **Bug A — "Guardando…" colgado.** `PurchaseRepositoryFirestoreImpl.add()`
  hacía `collection.document(...).set(data).await()`. Por contrato del
  SDK de Firestore, ese `Task` solo resuelve cuando el server confirma —
  offline queda colgado para siempre, la corrutina del `onSave()` nunca
  termina, y `isSaving` no vuelve a `false`.
  - **Fix:** fire-and-forget. La escritura sigue persistiendo
    sincronamente en el cache local (los listeners la ven de inmediato),
    pero `add()` ya no espera el ack del server. `isSaving` vuelve a
    `false`, `saveCompleted` se prende, y la pantalla se cierra en menos
    de 2 s aunque no haya red. Si el server después rechaza la
    escritura, se loggea con TAG `PurchaseRepoFirestore`.
- **Bug B — Badge "pendiente" para siempre.** `observeRecentWithPending`
  usaba `.snapshots()`, que por default es `MetadataChanges.EXCLUDE`.
  Cuando el server confirmaba la escritura pendiente, el único cambio
  era `metadata.hasPendingWrites: true → false` (metadata-only), y con
  EXCLUDE ese evento **no se emitía**. El `Flow` se quedaba con la
  última emisión donde `isPending = true` aunque el doc sí hubiera
  sincronizado.
  - **Fix:** cambiar ese query a `.snapshots(MetadataChanges.INCLUDE)`.
    Ahora el flip metadata se emite, el VM ve `isPending = false`, y
    el badge se quita.
- **Verificación en desarrollo:** `./gradlew assembleDebug` ✓; tests de
  reglas del emulador 24/24 ✓.
- **Pendiente:** re-correr en el mismo Xiaomi 15T con Melanie.
  Esperado: (1) el botón Guardar offline cierra la pantalla en <2 s; (2)
  las 3 compras aparecen en "Últimas 5" con badge "pendiente"; (3) al
  recuperar red, los badges se quitan uno por uno en 10-30 s; (4) los
  docs aparecen en Firebase Console con `serverWrittenAt` poblado.
- **Si el paso 4 falla** (los docs no llegan al server tras recuperar
  red), entonces hay un tercer bug que el task 20 **no** cubre —
  probablemente cola offline corrupta, token de Auth expirado, o regla
  rechazando el write. En ese caso, anotar y abrir issue separado.

---

### CP-08 — Proveedor desactivado vs histórico

**Qué prueba:** que un proveedor desactivado deje de aparecer en el
dropdown del muelle, pero que las compras viejas asociadas a él
sigan mostrando su nombre correctamente.

**Preparación:** necesitamos un proveedor que **esté desactivado** y
que tenga **al menos una compra histórica** asociada a él. El seed
del task 17 ya creó `suppliers/frutas-selectas-sa` como inactivo,
pero probablemente no tiene compras todavía. Antes de empezar:

1. Sesiónate como **ADMIN**.
2. Ve a la pestaña **Proveedores**, busca "Frutas Selectas SA".
3. Si está desactivado, **actívalo temporalmente** (edita → desliza el
   switch `isActive` a true → guarda).
4. Cierra sesión, entra como **OP-MEL**, captura **una compra** contra
   "Frutas Selectas SA" (1 tonelada, sin precio, hoy).
5. Cierra sesión, vuelve a entrar como **ADMIN**, vuelve a
   **desactivar** "Frutas Selectas SA".
6. Cierra sesión, entra como **OP-MEL** para arrancar el caso.

**Pasos:**

| # | Acción | Resultado esperado |
|---|---|---|
| 1 | Sesionada como **OP-MEL**, abre el FAB (+) → mira el dropdown de proveedores | "Frutas Selectas SA" **no** aparece en la lista |
| 2 | Cancela. Ve al Historial. Busca la compra que capturaste en la preparación | La compra aparece con el nombre `Frutas Selectas SA` legible y correcto, **a pesar** de que el proveedor está desactivado |
| 3 | Cierra sesión, entra como **ADMIN**, ve a Reportes | El proveedor desactivado puede o no aparecer en el "Top 5 del mes" dependiendo de cuántas toneladas tiene — no importa para este caso. Lo que importa es que el nombre histórico sea legible |

**Resultado:** `[x] Pasa` `[ ] Falla`

**Notas:**

```
Funcionamiento correcto

```

---

### CP-09 — Reglas de seguridad: denegación de auto-ascenso

**Qué prueba:** que un operador **no pueda** modificar su propio
documento de `users/{uid}` para subirse a admin, crear usuarios, ni
escribir campos de retiro/promoción. Esto es lo que evita que cualquiera
con la APK se promueva o fabrique cuentas.

> **Este caso está cubierto al 100% por los tests automatizados del
> emulador**: ver `tests/rules/rules.test.js` → `users › operator
> cannot promote self to admin`, `operator cannot create user documents`
> y `operator cannot write own privileged retirement fields`. La corrida
> más reciente debe reportar `tests 24, pass 24, fail 0`. Para esta
> corrida manual:
>
> **Marca este caso como ✅ "Cubierto por tests automatizados"** y
> pasa al CP-10. No tiene caso reproducirlo manualmente — necesitarías
> un cliente Firestore externo, y la regla ya quedó verificada.

**Resultado:** `[X] Cubierto por tests automatizados`

**Notas (opcional, si quieres verificar igual desde la consola):**

```


```

---

### CP-10 — Reglas de seguridad: escritura no autorizada en `suppliers`

**Qué prueba:** que un operador no pueda crear ni modificar
proveedores. Solo el admin puede tocar `suppliers/*`.

> **Igual que CP-09: cubierto por los tests automatizados.** Ver
> `tests/rules/rules.test.js` → casos `suppliers › operator cannot
> write suppliers` y `suppliers › operator cannot edit suppliers`.
>
> **Marca este caso como ✅ "Cubierto por tests automatizados"** y
> sigue con el CP-11.

**Resultado:** `[X] Cubierto por tests automatizados`

**Notas (opcional):**

```


```

---

### CP-11 — Zona horaria del `dateKey`

**Qué prueba:** que el `dateKey` (la cadena `"YYYY-MM-DD"` que se
usa para agrupar compras por día) se calcule en zona local del
operador al momento de capturar — **no en UTC** — y que no se
recalcule si después cambias la zona horaria del teléfono.

**Preparación:** ten claro qué hora es ahora en Ciudad de México
(UTC−6). Este caso es más fácil de correr en horas de la noche
(entre las 18:00 y las 23:59 hora local) porque en UTC ya cambió de
día y se ve claramente la diferencia.

**Pasos:**

| # | Acción | Resultado esperado |
|---|---|---|
| 1 | Verifica que el teléfono está en zona horaria **"América / Ciudad de México"** (UTC−6) | Confirmado en Ajustes → Sistema → Fecha y hora |
| 2 | Sesionada como **OP-MEL**, captura una compra **dejando la fecha por defecto** (la de hoy) | Vuelves al Dashboard |
| 3 | Abre Firestore Console y mira el doc nuevo | `dateKey == "YYYY-MM-DD"` donde `YYYY-MM-DD` es **la fecha local mexicana de hoy**. Por ejemplo, si capturaste el 30 de mayo a las 22:30 hora MX, `dateKey == "2026-05-30"` — aunque en UTC ya sean las 04:30 del 31 de mayo |
| 4 | Cambia la zona horaria del teléfono a **UTC** (Ajustes → Fecha y hora → desactivar automático → Londres / GMT) | El teléfono ahora muestra hora UTC |
| 5 | Vuelve a abrir la app, busca la compra en el Historial, vuelve a abrir Firestore | El `dateKey` del doc **no cambió** — sigue con la fecha local mexicana de cuando lo capturaste (`"2026-05-30"` en el ejemplo). El campo se decidió al escribir, no al leer |
| 6 | Restaura el teléfono a zona "América / Ciudad de México" | Limpieza |

> **Si en el paso 3 ves `dateKey == "2026-05-31"` cuando lo
> capturaste a las 22:30 del 30 hora local**, la app está calculando
> el dateKey en UTC en vez de zona local. Anótalo como falla — es un
> bug serio que rompería los reportes diarios.

**Resultado:** `[x] Pasa` `[ ] Falla`

**Notas:**

```
Funcionamiento correcto

```

---

### CP-12 — Visibilidad de pestañas por rol

**Qué prueba:** que la barra inferior solo muestre las pestañas
"Proveedores" y "Usuarios" cuando el rol es admin. Esto es
**conveniencia de UI** solamente — la autorización real está en las
reglas. Pero es lo que hace que la app se sienta "limpia" para el
operador.

**Preparación:** ninguna. Si ya cerraste sesión al final del CP-11,
empieza desde la pantalla de Login.

**Pasos:**

| # | Acción | Resultado esperado |
|---|---|---|
| 1 | Inicia sesión como **OP-MEL** | Barra inferior: **Dashboard / Compras / Reportes**. Las pestañas "Proveedores" y "Usuarios" **no** están |
| 2 | Cierra sesión, inicia como **ADMIN** | Barra inferior: **Dashboard / Compras / Proveedores / Usuarios / Reportes** (las 5) |

**Resultado:** `[x] Pasa` `[ ] Falla`

**Notas:**

```
Funcionamiento correcto

```

---

### CP-13 — Admin crea Operador desde la app

**Qué prueba:** que un Admin autenticado pueda crear una cuenta de
Operador desde la pestaña Usuarios, sin abrir auto-registro público.

**Preparación:** sesionada como **ADMIN**, con internet. Define un correo
único para **OP-TEMP** y una contraseña temporal de al menos 6 caracteres.

**Pasos:**

| # | Acción | Resultado esperado |
|---|---|---|
| 1 | Entra a la pestaña **Usuarios** | Ves el roster de Operadores activos |
| 2 | Pulsa **Operador** | Se abre el formulario "Nuevo operador" |
| 3 | Captura nombre, correo de **OP-TEMP** y contraseña temporal | El formulario acepta los datos |
| 4 | Pulsa **Crear operador** | Regresas al roster; aparece el nuevo Operador |
| 5 | Abre Firestore Console → `users` y busca el nuevo doc | Tiene `email`, `displayName`, `role = "operator"`, `accountCreatedAt`, `disabledAt = null`, `retiredAt = null` |
| 6 | Cierra sesión e inicia con **OP-TEMP** | Entra como Operador; no ve Proveedores ni Usuarios |

**Resultado:** `[X] Pasa` `[ ] Falla`

**Notas:**

```
Funciona Correctamente
```

---

### CP-14 — Admin crea otro Admin con re-autenticación

**Qué prueba:** que crear otro Admin requiera confirmar la contraseña del
Admin actuante y que el nuevo usuario quede con rol admin.

**Preparación:** sesionada como **ADMIN**, con internet. Define un correo
único para **ADMIN-TEMP** y una contraseña temporal de al menos 6
caracteres.

**Pasos:**

| # | Acción | Resultado esperado |
|---|---|---|
| 1 | En **Usuarios**, pulsa **Admin** | Se abre "Nuevo admin" |
| 2 | Captura nombre, correo de **ADMIN-TEMP**, contraseña temporal y una contraseña Admin incorrecta | Al pulsar **Crear admin**, aparece error; no se crea el usuario |
| 3 | Repite con la contraseña correcta de **ADMIN** | Regresas al roster; aparece mensaje "Administrador creado" |
| 4 | Abre Firestore Console → `users` y busca el nuevo doc | Tiene `role = "admin"`, `accountCreatedAt`, `disabledAt = null`, `retiredAt = null` |
| 5 | Cierra sesión e inicia con **ADMIN-TEMP** | Entra como Admin; ve Proveedores y Usuarios |

**Resultado:** `[X] Pasa` `[ ] Falla`

**Notas:**

```
Funciona correctamente
```

---

### CP-15 — Promoción de Operador a Admin

**Qué prueba:** que un Admin pueda promover un Operador usando el flujo
de retiro/recreación: se retira el login viejo, se crea un Admin nuevo
con el mismo correo y las compras históricas conservan el `uid` viejo.

**Preparación:** usa **OP-TEMP** creado en CP-13, no **OP-MEL** ni
**OP-MAX**. Antes de promoverlo, inicia sesión como **OP-TEMP** y captura
una compra sencilla para que exista una compra histórica con
`createdBy = oldOperatorUid`.

**Pasos:**

| # | Acción | Resultado esperado |
|---|---|---|
| 1 | Como **ADMIN**, entra a **Usuarios** y localiza **OP-TEMP** | El Operador aparece en el roster |
| 2 | Pulsa el icono de promover del renglón de **OP-TEMP** | Se abre "Promover operador" con su nombre/correo |
| 3 | Captura una contraseña incorrecta de Operador y la contraseña correcta de Admin | La promoción falla; **OP-TEMP** sigue como Operador |
| 4 | Repite con contraseña correcta de Operador y contraseña Admin incorrecta | La promoción falla; no se crea Admin nuevo |
| 5 | Repite con ambas contraseñas correctas | Regresas al roster; aparece mensaje de éxito |
| 6 | En Firestore revisa el `users/{oldOperatorUid}` de **OP-TEMP** | Conserva `role = "operator"`, pero ahora tiene `disabledAt`, `retiredAt`, `promotedToUid` y `authEmailRetiredTo` |
| 7 | Revisa `users/{promotedToUid}` | Tiene el correo original de **OP-TEMP**, `role = "admin"`, `promotedFromUid = oldOperatorUid` |
| 8 | Inicia sesión con el correo original de **OP-TEMP** y su contraseña | Entra como Admin nuevo; ve Proveedores y Usuarios |
| 9 | Revisa la compra histórica creada antes de promover | Su `createdBy` sigue siendo `oldOperatorUid`; no se reescribió a `promotedToUid` |

**Resultado:** `[X] Pasa` `[ ] Falla`

**Notas:**

```


```

---

### CP-16 — Usuarios retirados y writes privilegiados

**Qué prueba:** que la seguridad no dependa de ocultar botones: ni
Operadores ni Admins pueden crear/modificar documentos `users/*`
directamente desde cliente, y un Operador retirado no puede seguir
operando aunque conserve un token viejo.

> Este caso es automatizado. Córrelo desde la raíz del repo:
>
> ```sh
> firebase emulators:exec --only firestore 'cd tests/rules && npm test'
> ```
>
> Debe terminar con `tests 24`, `pass 24`, `fail 0`.

**Resultado:** `[ ] Cubierto por tests automatizados` `[ ] Falla`

**Notas:**

```


```

---

## 4. Si algo falla

1. **Captura pantalla** de lo que viste cuando falló (en Android,
   botón de encendido + bajar volumen).
2. **Anota** en el campo de Notas del caso:
   - Qué hiciste exactamente.
   - Qué esperabas ver.
   - Qué viste en su lugar.
   - Hora aproximada (para que yo lo cruce con logs de Firebase si
     hace falta).
3. **No re-intentes inmediatamente** — sigue con el siguiente caso.
   Cuando termines toda la corrida, me mandas el reporte completo y
   yo investigo lo que falló.
4. **Si lo que falló es algo de seguridad** (por ejemplo, en CP-05 un
   operador sí pudo editar la compra de otro operador), anótalo con
   **prioridad alta** — eso bloquea la entrega.

## 5. Hoja de resultados

Llena esta tabla al terminar. Pasa = "vi exactamente el resultado
esperado". Falla = "no lo vi" o "vi algo distinto". Si dudas, pon
"Parcial" en la columna de Notas y descríbelo.

| CP | Qué prueba | Resultado | Notas breves |
|---|---|---|---|
| CP-01 | Auth (login, logout, barra por rol) | `[Si] Pasa` `[ ] Falla` | |
| CP-02 | Captura de compra normal | `[ ] Pasa` `[ ] Falla` |Si|
| CP-03 | Captura sin precio | `[Si] Pasa` `[ ] Falla` | |
| CP-04 | Captura contra UNREGISTERED | `[Si] Pasa` `[ ] Falla` | |
| CP-05 | Ventana de edición 24h | `[Si] Pasa` `[ ] Falla` | |
| CP-06 | Soft-delete | `[ ] Pasa` `[Si] Falla` → corregido en task 20 (2026-05-31), re-corrida pendiente | |  
    Falla original: la compra eliminada sigue apareciendo en Historial/Todos hasta cambiar entre pestañas internas. Fix: `softDelete()` ahora escribe `deletedAt` con `Timestamp.now()` del cliente para que el caché lo vea no-null inmediato. Tests de reglas 24/24 ✓. Pendiente re-corrida en Xiaomi 15T.
| CP-07 | Captura offline + sync | `[ ] Pasa` `[Si] Falla` → corregido en task 20 (2026-05-31), re-corrida pendiente | |
    Falla original: al guardar offline el botón se queda en "Guardando..."; la compra aparece como pendiente, pero al recuperar internet no se sincroniza y queda pendiente permanentemente. Fix: (A) `add()` fire-and-forget en lugar de `.set().await()` para no colgar offline; (B) `observeRecentWithPending` usa `MetadataChanges.INCLUDE` para que el flip `hasPendingWrites: true→false` emita. Pendiente re-corrida en Xiaomi 15T.
| CP-08 | Proveedor desactivado vs histórico | `[Si] Pasa` `[ ] Falla` | |
| CP-09 | Auto-ascenso denegado | `[X] Cubierto por tests automatizados` | n/a manual |
| CP-10 | Escritura no autorizada en suppliers | `[X] Cubierto por tests automatizados` | n/a manual |
| CP-11 | Zona horaria del `dateKey` | `[Si] Pasa` `[ ] Falla` | |
| CP-12 | Visibilidad de pestañas por rol | `[Si] Pasa` `[ ] Falla` | Re-correr por nueva pestaña Usuarios |
| CP-13 | Admin crea Operador | `[ ] Pasa` `[ ] Falla` | |
| CP-14 | Admin crea otro Admin | `[ ] Pasa` `[ ] Falla` | |
| CP-15 | Promoción Operador → Admin | `[ ] Pasa` `[ ] Falla` | |
| CP-16 | Usuarios retirados / writes privilegiados | `[ ] Cubierto por tests automatizados` `[ ] Falla` | |

**Dispositivo usado:** ___Telefono Xiaomi 15T____________

**Versión de Android:** __16____________________

**APK probado:** `mangos-v1.0.apk` (SHA: lo te paso cuando lo
genere; pon aquí el que recibiste) LO RECIBÍ

**Fecha de la corrida:** ___3o/05/2026_y 31/05/2026_____________

**Tiempo total invertido:** ___3 Horas aproximandamente____________

## 6. Mini-glosario

- **APK:** el archivo instalador de Android.
- **FAB:** "Floating Action Button" — el botón redondo flotante (en
  esta app, el "+" en el Dashboard).
- **Dropdown:** lista desplegable.
- **Firestore Console:** `https://console.firebase.google.com` →
  proyecto `mangos-372bd` → Firestore Database. Es donde miras los
  documentos directamente sin pasar por la app.
- **uid:** identificador único de Firebase Auth para cada usuario.
  Cada cuenta tiene uno distinto. Lo ves en `users/{uid}` y en el
  campo `createdBy` de las compras.
- **`dateKey`:** cadena `"YYYY-MM-DD"` que se guarda con cada compra
  para agruparlas por día calendario sin hacer matemática de
  timestamps. CP-11 lo prueba.
- **Soft-delete:** "borrar" pero no físicamente — solo se pone una
  marca `deletedAt` y se oculta de la UI. El doc sigue ahí. CP-06 lo
  prueba.
- **Pending / pendiente:** indicador en una compra que dice "esto se
  capturó offline y todavía no llega al servidor". CP-07 lo prueba.
- **Retirado:** cuenta de Operador que ya no puede operar porque fue
  promovida. Su documento `users/{oldUid}` queda para auditoría, pero el
  login viejo se desactiva. CP-15/CP-16 lo prueban.
- **Promoción:** flujo Admin-only que retira un Operador y crea un nuevo
  Admin con el mismo correo. Las compras históricas conservan el `uid`
  viejo. CP-15 lo prueba.

---

**Cuando termines:** mándame esta hoja llena (o haz commit + push si
estás cómoda con git) y yo reviso. Si todo pasa, le damos luz verde
al APK y entregamos el 1 de junio.

Gracias Mel — esto es la última red de seguridad antes de la
entrega.
