# Glosario de Dominio

Traducción al español del glosario canónico `docs/CONTEXT.md`.
Define los términos del dominio de Mangos USA. Los detalles de
implementación van en otros entregables; este archivo es solo el
vocabulario.

---

## Compra (`Purchase`)

Un **evento de recepción**: un registro de que cierta cantidad de mangos
llegó físicamente de un proveedor en un día dado. Se crea cuando un
camión es descargado y pesado. Reemplaza una nota adhesiva en el pizarrón
de bodega.

Una vez registrada, una Compra es un hecho histórico sobre el ingreso de
inventario. **No** es una orden de compra, un compromiso, ni una partida
contable — esos son conceptos separados que la app aún no modela.

- **¿Mutable?** Solo correcciones (ej. typo en el peso). No es libremente
  editable.
- **¿Borrable?** Solo soft-delete. El borrado físico destruiría el rastro
  de auditoría.

### Campos con significado no obvio

- **`date`** — la fecha **recibido-en**: cuándo llegaron los mangos a la
  bodega. Distinta de los timestamps de captura.

### Tres timestamps, tres propósitos

Una Compra carga tres timestamps distintos. No deben colapsarse.

- **`date`** — la fecha **recibido-en**: cuándo llegaron los mangos
  físicamente. Establecida por el Operador, puede retroactivarse. Es la
  fecha que usan los Reportes para "toneladas recibidas el día X."
- **`enteredAt: Timestamp`** — el **reloj del cliente** al momento que el
  Operador pulsó guardar en el dispositivo. Solo para display
  ("registrado el viernes 4:00pm"). Sobrevive a la cola offline — refleja
  cuándo el humano actuó, no cuándo el servidor recibió la escritura.
- **`serverWrittenAt: Timestamp`** — `FieldValue.serverTimestamp()`. El
  momento que Firestore aceptó la escritura. Autoritativo para la ventana
  de edición de 24h del Operador en las reglas de seguridad. Nunca se
  muestra al usuario.

No hay campo `createdAt`. El `createdAt` del plan original era ambiguo
entre estos tres significados.

Patrón común esperado: `date = viernes`, `enteredAt = viernes 4pm`
(offline en el teléfono), `serverWrittenAt = lunes 9am` (cuando volvió la
conectividad).

### `dateKey` — el campo bucket-por-día

Cada Compra carga `dateKey: String` formateado `"YYYY-MM-DD"`, calculado
desde `date` en la **zona horaria local** del Operador al momento de
escritura. Las consultas bucket-por-día (Dashboard "hoy", agregación
diaria de Reportes) usan match exacto sobre `dateKey`, **nunca**
matemática de rangos UTC sobre `date`.

Por qué un string denormalizado en lugar de computar límites del día
desde `date`: los Timestamps de Firestore son UTC; la Ciudad de México es
UTC−6 sin horario de verano desde 2022. Una consulta ingenua sobre rango
de medianoche UTC silenciosamente incluye o excluye compras del día
calendario incorrecto. `dateKey` evita el problema completamente — el
bucket de día se decide una vez, en el muelle, en la zona horaria que le
importa al humano.

Si `date` se edita, `dateKey` debe recalcularse en la misma transacción.

### Dinero

Todos los precios de Compra están en **pesos mexicanos (MXN)**. No hay
override de moneda por Compra; la corporación opera en México pese al
branding "USA", y todos los proveedores facturan en MXN.

Almacenado como **`Long centavos`** (ej. `123456` = `$1,234.56`).
Firestore almacena todos los números como IEEE 754 doubles, así que
cents-as-integer es la única forma de obtener precisión exacta y
agregación exacta en Reportes. La UI siempre formatea desde centavos en
el límite; el código de dominio nunca usa `Double` para dinero.

Nombre canónico del campo: `pricePerTonCentavos: Long`.

### El precio es opcional

Una Compra puede registrarse en el muelle **sin precio**. El precio
suele negociarse por separado de la entrega; bloquear la descarga del
camión esperando captura de precio derrotaría el flujo en el muelle. El
Administrador (o el mismo Operador después) puede backfill
`pricePerTonCentavos` después. Los Reportes deben tolerar Compras sin
precio (excluirlas de totales de gasto; aún contarlas en totales de
tonelaje).

### Dónde y cuándo se crea

En el muelle, en un teléfono, **conforme el camión se descarga**. El
Operador está parado junto a la báscula, selecciona el proveedor, captura
el peso, pulsa guardar. La conectividad en bodega no es confiable, así
que la app debe aceptar capturas de Compra offline y sincronizarlas
cuando vuelve la conectividad. Un estado de "pendiente de sincronizar" es
una preocupación de UI de primera clase, no un afterthought.

### Los campos de nombre denormalizados son históricos, nunca retro-rellenados

`supplierName` y `createdByName` se denormalizan sobre cada Compra al
**momento de escritura** y **nunca se actualizan** cuando el Proveedor o
Usuario subyacente cambia después. Una Compra es un hecho histórico —
incluyendo a quién se le atribuyó y qué nombre de proveedor estaba en los
libros ese día. Los renombres afectan solo Compras nuevas.

Regla práctica: cuando un Proveedor o Usuario se edita, **no** se hace
fan-out de actualizaciones a las Compras existentes.

### Esquema de soft-delete

- `deletedAt: Timestamp?` — null en Compras vivas; establecido con
  `Timestamp.now()` del cliente al borrar para que el cache local lo vea
  no-null inmediatamente y oculte la fila sin esperar el ack del servidor.
  El reloj autoritativo de auditoría sigue siendo `serverWrittenAt`.
- `deletedBy: String?` — userId del usuario que borró (por ADR-0002,
  siempre un Admin o el Operador original dentro de la ventana de 24h).
- Todas las consultas de Compra por defecto filtran
  `where deletedAt == null`. Las filas borradas permanecen en Firestore
  para auditoría; no hay UI v1 para navegarlas.
- El borrado físico nunca se usa.

Los Proveedores usan `isActive: Boolean` en su lugar — la desactivación
es reversible y oculta al Proveedor del dropdown del muelle sin afectar
la resolución histórica de Compras. Los Usuarios no se borran físicamente:
en el caso especial de promoción Operador→Admin, el login viejo se retira
con `disabledAt`/`retiredAt` y queda como evidencia histórica.

---

## Operador

Un trabajador de bodega que registra Compras en el muelle. Cada Operador
tiene su propio teléfono de empresa y su propia cuenta de Firebase Auth;
`createdBy` en una Compra es el userId del Operador y se trata como
atribución precisa.

Un modelo de tableta compartida (un dispositivo, muchos humanos cambiando
vía selector) se consideró y se difirió — ver ADR-0001.

## Administrador

Un usuario que puede gestionar el roster de Proveedores y el roster de
Operadores. También puede registrar Compras. No hay un rol "supervisor"
separado; admin/operator es el único split de roles.

## Creación de cuentas

**No hay auto-registro.** Ni los Operadores ni los Administradores pueden
darse de alta a sí mismos. El primer Administrador se crea manualmente en
Firebase para bootstrap; después, la creación de cuentas ocurre desde la
pestaña **Usuarios** por acción de un Administrador autenticado.

Esto también resuelve la pregunta de bootstrap "¿quién es el primer
Administrador?": quien configure el proyecto de Firebase es el primer
Administrador, por virtud de tener acceso a la Consola.

Un Administrador autenticado puede registrar Operadores, administrar el
roster de Operadores y registrar otro Administrador. Para dar de alta otro
Administrador debe reingresar sus credenciales como confirmación.

La promoción de Operador a Administrador es un flujo explícito, no una edición
directa de `role`: el Admin selecciona al Operador, el sistema
retira/desactiva la cuenta de Operador y crea una nueva cuenta de
Administrador con el mismo correo electrónico. Para completar el cambio, el
Operador promovido debe reingresar su contraseña y el Admin actuante debe
reingresar sus credenciales como confirmación final. Las Compras históricas
siguen apuntando al `uid` original del Operador.

## Política de autorización

El enforcement de roles es **del lado del servidor**, vía Firestore
Security Rules. La UI cliente oculta acciones admin-only por ergonomía,
pero la fuente de verdad son las reglas. Un usuario no puede ascenderse a
admin editando su propio campo de rol; las reglas lo prohíben. Ver
ADR-0002.

Resumen de política:

- `users/{uid}`: lectura = propio doc O admin, siempre que la cuenta esté
  activa; escritura de cliente = solo `displayName`. La creación de
  usuarios, cambio de `role` y campos de retiro/promoción los escribe
  Cloud Functions con Admin SDK.
- `suppliers/*`: lectura = cualquier autenticado; escritura = solo admin.
- `purchases/*`: lectura = cualquier autenticado; creación = cualquier
  autenticado; update/delete = admin O el creador original dentro de
  24h de `serverWrittenAt` (ventana de typo-fix del Operador).

## Proveedor (`Supplier`)

Una contraparte a la que la bodega le compra mangos. Creado y editado
solo por Administradores — los Operadores no pueden crear Proveedores,
ni siquiera en el muelle.

### Activo vs inactivo

`isActive: Boolean` es un flag de desactivación suave. Los Proveedores
inactivos no aparecen en el dropdown del muelle para nuevas Compras pero
siguen resolviendo correctamente sobre filas de Compras históricas (su
nombre está denormalizado de todos modos). Un Proveedor nunca se borra
físicamente.

### Proveedor no registrado (escape hatch)

Un documento de Proveedor reservado con `id = "UNREGISTERED"` y nombre
"Proveedor no registrado" existe en todo momento. Cuando un camión llega
de un Proveedor que aún no está en el roster, el Operador registra la
Compra contra `UNREGISTERED` y escribe el nombre real de la contraparte
en `supplierNoteFreeform` en la Compra. El Administrador reconcilia
estos después (a) creando un Proveedor real y editando el `supplierId`
de la Compra, o (b) dejándolo para one-offs de bajo volumen.

`supplierNoteFreeform` es `null` para Compras normales y solo se
establece cuando `supplierId == "UNREGISTERED"`.
