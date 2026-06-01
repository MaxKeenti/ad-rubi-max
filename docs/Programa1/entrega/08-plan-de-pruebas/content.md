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

## 2. Casos de prueba manuales y de reglas

### CP-01: Flujo de autenticación

| Paso | Acción | Resultado esperado |
|---|---|---|
| 1 | Existe al menos una cuenta Admin bootstrap creada desde Firebase Console | Cuenta visible en Firestore en `users/{uid}` con `role = "admin"` |
| 2 | Operador abre la app por primera vez | Aparece pantalla de Login (sin opción de Registro) |
| 3 | Operador captura credenciales correctas | Aterriza en Dashboard como Operador |
| 4 | Operador cierra sesión desde el menú overflow del Dashboard | Regresa a Login |
| 5 | Admin inicia sesión con sus credenciales | Aterriza en Dashboard con las pestañas **Proveedores** y **Usuarios** visibles |

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
| 3 | Operador intenta crear otro `users/{uid}` o escribir `disabledAt`, `retiredAt`, `promotedToUid` | La escritura es **denegada**; esos campos solo los escribe Cloud Functions con Admin SDK |
| 4 | Operador puede actualizar su `displayName` | Permitido |

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
| 1 | Operador inicia sesión | Barra inferior muestra Dashboard, Compras, Reportes (sin Proveedores ni Usuarios) |
| 2 | Admin inicia sesión | Barra inferior muestra Dashboard, Compras, Proveedores, Usuarios, Reportes |

### CP-13: Admin crea Operador desde la app

| Paso | Acción | Resultado esperado |
|---|---|---|
| 1 | Admin entra a la pestaña **Usuarios** | Se muestra el roster de Operadores activos |
| 2 | Pulsa **Operador** y captura nombre, correo nuevo y contraseña inicial | El formulario valida correo y contraseña mínima |
| 3 | Pulsa **Crear operador** | La cuenta Auth se crea, `users/{newUid}` existe con `role = "operator"` y el roster se refresca |
| 4 | Cierra sesión e inicia con la cuenta nueva | Aterriza como Operador; no ve Proveedores ni Usuarios |

### CP-14: Admin crea otro Admin con re-autenticación

| Paso | Acción | Resultado esperado |
|---|---|---|
| 1 | Admin entra a **Usuarios** → **Admin** | Se abre el formulario de alta de Admin |
| 2 | Captura nombre, correo nuevo, contraseña inicial y una contraseña Admin incorrecta | La operación falla; no aparece `users/{newUid}` con rol admin |
| 3 | Repite con la contraseña correcta del Admin actuante | La cuenta se crea con `role = "admin"` |
| 4 | Cierra sesión e inicia con el nuevo Admin | Ve Proveedores y Usuarios en la barra inferior |

### CP-15: Promoción de Operador a Admin

| Paso | Acción | Resultado esperado |
|---|---|---|
| 1 | Admin crea o selecciona un Operador de prueba con una compra histórica | La compra tiene `createdBy = oldOperatorUid` |
| 2 | En **Usuarios**, pulsa el icono de promover en ese Operador | Se abre confirmación con resumen del Operador |
| 3 | Captura contraseña incorrecta del Operador o del Admin | La promoción falla sin cambiar roles ni crear nuevo Admin |
| 4 | Captura contraseña correcta del Operador y del Admin actuante | El viejo `users/{oldUid}` queda con `disabledAt`, `retiredAt`, `promotedToUid`; se crea `users/{newUid}` con `role = "admin"` y `promotedFromUid = oldUid` |
| 5 | Intenta iniciar sesión con la cuenta vieja y luego con el correo promovido | La cuenta vieja queda retirada; el correo original inicia como Admin nuevo |
| 6 | Revisa la compra histórica | Conserva `createdBy = oldOperatorUid`; no se reescribe a `newUid` |

### CP-16: Usuarios retirados y writes privilegiados

| Paso | Acción | Resultado esperado |
|---|---|---|
| 1 | Con tests de reglas, simula un Operador retirado con token antiguo | No puede leer su doc ni crear compras |
| 2 | Con tests de reglas, intenta crear usuarios o cambiar roles desde cliente Admin/Operador | Denegado; solo Functions/Admin SDK puede hacerlo |
| 3 | Ejecuta `firebase emulators:exec --only firestore "npm --prefix tests/rules test"` | Todos los tests pasan |

## 3. Pruebas automatizadas

### Compilación y pruebas unitarias

```sh
cd Programa1
./gradlew :app:assembleDebug      # debe terminar en BUILD SUCCESSFUL
./gradlew :app:testDebugUnitTest  # pruebas unitarias Kotlin
```

Estos se corren antes de la entrega y antes de cambios relevantes.
Forman la red de seguridad mínima del cliente Android.

### Reglas de Firestore con el emulador

```sh
firebase emulators:exec --only firestore "npm --prefix tests/rules test"
```

Los tests cubren los casos críticos de los CP-09, CP-10 y CP-16 más:

- Lectura permitida para usuarios autenticados, denegada para anónimos.
- Creación de Purchase permitida solo si `createdBy == auth.uid`.
- Update de Purchase respetando la ventana de 24h medida contra
  `serverWrittenAt`.
- Denegación de creación directa de `users/*`, cambio directo de `role`,
  y escritura directa de campos privilegiados (`disabledAt`, `retiredAt`,
  `promotedToUid`).
- Bloqueo de usuarios retirados aunque conserven un token antiguo.

## 4. Criterios de aceptación para entrega

La app se considera lista para entrega cuando:

- [ ] CP-01 a CP-15 pasan en emulador y en al menos un dispositivo físico
      Android; CP-16 pasa en el emulador de reglas.
- [ ] `./gradlew assembleDebug` termina sin errores.
- [ ] El APK firmado se instala y arranca en un dispositivo limpio.
- [ ] Las reglas de Firestore están desplegadas en el proyecto de
      producción.
- [ ] Los entregables 01–09 están completos y compilan a PDF sin warnings
      en Typst.
- [ ] El README del proyecto explica cómo ejecutar el APK y dónde
      encontrar los entregables.
