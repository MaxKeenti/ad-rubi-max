# Plan de Pruebas

## 1. Estrategia

Dado el presupuesto (≈20.5 horas para implementación + documentación), la
estrategia de pruebas es **pragmática y orientada a riesgos**:

- **Pruebas manuales** sobre emulador y dispositivo físico, cubriendo los
  flujos críticos del usuario.
- **Pruebas de reglas de seguridad** del lado del servidor con el
  Emulador de Firestore (no opcional — sin esto no podemos afirmar que
  ADR-0002 sea real).
- **Pruebas automatizadas mínimas**: que el proyecto compile
  (`./gradlew assembleDebug`) y que lint no devuelva errores críticos
  (`./gradlew lint`).

No se hacen pruebas unitarias exhaustivas por presupuesto. Si la app
falla en un flujo, lo agarra el test manual; si las reglas fallan, lo
agarra el test del emulador.

## 2. Casos de prueba manuales

### CP-01: Flujo de autenticación

| Paso | Acción | Resultado esperado |
|---|---|---|
| 1 | Admin crea una cuenta en la Consola de Firebase con rol "operator" | Cuenta visible en Firestore en `users/{uid}` con `role = "operator"` |
| 2 | Operador abre la app por primera vez | Aparece pantalla de Login (sin opción de Registro) |
| 3 | Operador captura credenciales correctas | Aterriza en Dashboard como Operador |
| 4 | Operador cierra sesión desde el menú overflow del Dashboard | Regresa a Login |
| 5 | Admin inicia sesión con sus credenciales | Aterriza en Dashboard con la pestaña **Proveedores** visible |

### CP-02: Captura de compra normal

| Paso | Acción | Resultado esperado |
|---|---|---|
| 1 | Operador pulsa FAB en Dashboard | Se abre AddEditPurchaseScreen |
| 2 | Selecciona un proveedor activo | Dropdown muestra el nombre seleccionado |
| 3 | Captura 5.5 toneladas, precio 12,500 MXN/ton, fecha de hoy | Campos validan correctamente |
| 4 | Pulsa Guardar | Vuelve a Dashboard; la compra aparece en "últimas 5" |
| 5 | Revisa Firestore | El documento existe con `dateKey` correcto, `pricePerTonCentavos = 1250000`, `enteredAt` y `serverWrittenAt` ambos seteados |

### CP-03: Captura sin precio

| Paso | Acción | Resultado esperado |
|---|---|---|
| 1 | Operador captura una compra dejando precio en blanco | El formulario permite guardar |
| 2 | Revisa Firestore | `pricePerTonCentavos == null` |
| 3 | Va a Reportes | Total de toneladas incluye esta compra; total de gasto la excluye; aparece "(1 entrada sin precio)" |

### CP-04: Captura contra UNREGISTERED

| Paso | Acción | Resultado esperado |
|---|---|---|
| 1 | Operador selecciona "Proveedor no registrado" en el dropdown | Aparece campo "Nombre del proveedor" |
| 2 | Captura "Mangos de Veracruz S.A." en el campo libre, completa toneladas, guarda | Compra creada con `supplierId = "UNREGISTERED"` y `supplierNoteFreeform = "Mangos de Veracruz S.A."` |
| 3 | Admin entra a Reportes / Historial | La compra aparece bajo "Proveedor no registrado" con la nota visible |

### CP-05: Ventana de edición de 24h del Operador

| Paso | Acción | Resultado esperado |
|---|---|---|
| 1 | Operador A captura una compra | OK |
| 2 | Operador A intenta editarla en los primeros 60 segundos | Edición permitida |
| 3 | Operador A intenta editar la compra de Operador B | Edición denegada por la regla (mensaje de permisos) |
| 4 | (Avanzando el reloj o usando una compra antigua del seed) Operador A intenta editar una compra propia con `serverWrittenAt` > 24h | Edición denegada |
| 5 | Admin edita la misma compra antigua | Permitido |

### CP-06: Soft-delete

| Paso | Acción | Resultado esperado |
|---|---|---|
| 1 | Admin elimina una compra (swipe-to-delete) | La compra desaparece del Historial y del Dashboard |
| 2 | Revisar Firestore | El documento sigue existiendo, ahora con `deletedAt != null` y `deletedBy = <adminUid>` |
| 3 | Cualquier consulta en la app | No la incluye |

### CP-07: Captura offline

| Paso | Acción | Resultado esperado |
|---|---|---|
| 1 | Operador activa modo avión en el teléfono | Sin conectividad |
| 2 | Captura 3 compras seguidas | Las 3 se guardan; el Dashboard muestra el indicador "pendiente" en cada una |
| 3 | Apaga modo avión | Las 3 se sincronizan; los indicadores "pendiente" desaparecen; `serverWrittenAt` queda poblado |

### CP-08: Proveedor desactivado vs histórico

| Paso | Acción | Resultado esperado |
|---|---|---|
| 1 | Admin desactiva un proveedor que tiene compras históricas | OK |
| 2 | Operador abre AddEditPurchase | El proveedor desactivado **no** aparece en el dropdown |
| 3 | Historial de compras | Las compras viejas siguen mostrando el nombre del proveedor desactivado correctamente |

### CP-09: Reglas de seguridad — denegación de auto-ascenso

| Paso | Acción | Resultado esperado |
|---|---|---|
| 1 | Operador inicia sesión | Rol "operator" |
| 2 | Operador intenta escribir su propio doc cambiando `role` a `"admin"` (vía emulador, consola web, o un cliente Firestore directo) | La escritura es **denegada** por las reglas |
| 3 | Operador puede actualizar su `displayName` | Permitido |

### CP-10: Reglas de seguridad — escritura no autorizada en `suppliers`

| Paso | Acción | Resultado esperado |
|---|---|---|
| 1 | Operador intenta crear un proveedor desde un cliente Firestore directo | Denegado |
| 2 | Operador intenta editar un proveedor existente | Denegado |
| 3 | Admin hace lo mismo | Permitido |

### CP-11: Zona horaria del `dateKey`

| Paso | Acción | Resultado esperado |
|---|---|---|
| 1 | Configura el teléfono con zona horaria de Ciudad de México (UTC−6) | OK |
| 2 | A las 23:30 local del 26 de mayo, captura una compra dejando fecha por defecto | `dateKey == "2026-05-26"` |
| 3 | Cambia la zona horaria del teléfono a UTC y revisa la misma compra en Firestore | `dateKey` permanece `"2026-05-26"` (se decidió al escribir, no se recalcula al leer) |

### CP-12: Visibilidad de pestañas por rol

| Paso | Acción | Resultado esperado |
|---|---|---|
| 1 | Operador inicia sesión | Barra inferior muestra Dashboard, Compras, Reportes (sin Proveedores) |
| 2 | Admin inicia sesión | Barra inferior muestra Dashboard, Compras, Proveedores, Reportes |

## 3. Pruebas automatizadas

### Compilación y lint

```sh
./gradlew assembleDebug   # debe terminar en BUILD SUCCESSFUL
./gradlew lint            # no errores críticos
```

Estos se corren antes de cada commit relevante. Forman la red de
seguridad mínima.

### Reglas de Firestore con el emulador

```sh
firebase emulators:start --only firestore
# en otra terminal, contra la URL del emulador:
firebase emulators:exec --only firestore "npm run test:rules"
```

Los tests cubren los casos críticos de los CP-09 y CP-10 más:

- Lectura permitida para usuarios autenticados, denegada para anónimos.
- Creación de Purchase permitida solo si `createdBy == auth.uid`.
- Update de Purchase respetando la ventana de 24h medida contra
  `serverWrittenAt`.

## 4. Criterios de aceptación para entrega

La app se considera lista para entrega cuando:

- [ ] Los 12 casos de prueba manuales pasan en emulador y en al menos un
      dispositivo físico Android.
- [ ] `./gradlew assembleDebug` termina sin errores.
- [ ] El APK firmado se instala y arranca en un dispositivo limpio.
- [ ] Las reglas de Firestore están desplegadas en el proyecto de
      producción.
- [ ] Los entregables 01–09 están completos y compilan a PDF sin warnings
      en Typst.
- [ ] El README del proyecto explica cómo ejecutar el APK y dónde
      encontrar los entregables.
