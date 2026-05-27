# Decisiones Arquitectónicas (ADRs)

Las decisiones arquitectónicas con alternativas válidas, costosas de
revertir, y que requieren contexto para entenderse se documentan como
**ADR (Architecture Decision Records)**. Decisiones más pequeñas viven en
el glosario (entregable 06) o en la arquitectura (entregable 03).

---

## ADR-0001 — Autenticación por dispositivo personal, no por tableta compartida

**Estado:** Aceptada
**Fecha:** 2026-05-26

### Contexto

Las compras se registran **en el muelle, conforme los camiones son
descargados** (ver glosario → "Purchase"). Ese flujo podría servirse con
dos modelos de identidad distintos:

- **(i) Dispositivo personal por Operador.** Cada Operador tiene su propio
  teléfono de empresa, inicia sesión una vez, permanece autenticado.
  `createdBy` en cada compra es el userId del Operador en Firebase Auth.
- **(ii) Tableta de bodega compartida.** Uno o dos dispositivos viven en
  el muelle; el humano de turno los usa. Firebase Auth identifica al
  *dispositivo*; una colección separada `operators` identifica al *humano*;
  un selector de operador corre al abrir la app o tras un timeout de
  inactividad, y el id seleccionado se vuelve `createdBy`.

La opción (ii) se alinea más cercanamente con cómo operan en la realidad
las bodegas de mango (los trabajadores no suelen tener smartphones
corporativos), pero requiere:

- Una segunda colección de identidad (`operators`) separada de `users`.
- Una UI de selector de operador + lógica de timeout de inactividad.
- Una UI administrativa para gestionar el roster de operadores.
- Texto en el documento de arquitectura explicando la separación
  dispositivo-vs-humano.

Costo agregado estimado: 4–6 horas, contra un presupuesto de 5 días
para dos personas.

### Decisión

Se adopta **(i) autenticación por dispositivo personal** para el release
inicial. `users` es una colección única; la identidad de Firebase Auth
*es* la identidad del Operador; `createdBy` apunta directamente a un
userId de Firebase.

### Consecuencias

**Positivas**

- El modelo de auth es el default de Firebase — sin código personalizado
  de sesión o selector.
- Ahorra ~4–6 horas, lo que es significativo sobre un presupuesto de
  5 días.
- Compatible hacia adelante con (ii): agregar un campo `operatorId` más
  tarde es aditivo, y los valores existentes de `createdBy` siguen siendo
  significativos como id de cuenta de dispositivo.

**Negativas**

- La demo descansa sobre el supuesto "cada trabajador tiene un teléfono de
  empresa." Esto es plausible pero no universal en esta industria. Debe
  declararse explícitamente en el documento de arquitectura.
- Si el despliegue real resulta ser de tableta compartida, `createdBy` se
  vuelve engañoso hasta que se implemente (ii).

### Disparadores para revisitar

Cambiar a (ii) si **cualquiera** de las siguientes condiciones se vuelve
verdadera:

- El target de despliegue se confirma como dispositivos compartidos
  (ej., el cliente dice "vamos a poner dos tabletas en el muelle, los
  trabajadores no van a tener teléfonos").
- Surgen disputas de atribución ("¿quién registró esta carga?") en la
  práctica.

### Alternativas consideradas

- **(iii) Sin atribución per-operador en absoluto** — eliminar
  `createdBy`, trackear solo el dispositivo. Rechazada: pierde el rastro
  de auditoría que justifica el encuadre completo de "reemplazar el
  pizarrón."

---

## ADR-0002 — Autorización del lado del servidor vía Firestore Security Rules

**Estado:** Aceptada
**Fecha:** 2026-05-26

### Contexto

La app tiene dos roles (admin, operator) con permisos diferenciados:
solo los administradores gestionan proveedores, solo los administradores
(o el Operador original dentro de una ventana corta) pueden editar o
eliminar compras, y ningún usuario puede ascenderse a admin.

Firebase Auth + Firestore es un modelo abierto cliente-servidor: la APK
embarca `google-services.json`, y cualquier cliente con credenciales
puede hablar directamente con Firestore. Ocultar UI administrativa en
Compose no autoriza nada — solo des-satura la pantalla.

Tres opciones consideradas:

- **(I) Firestore Security Rules.** Autorización enforzada del lado del
  servidor. Los roles se leen desde `users/{uid}.role`. Las reglas
  prohíben el auto-ascenso.
- **(II) Enforcement solo cliente + limitación documentada.** Ocultar
  UI administrativa; agregar una nota de "Consideraciones de seguridad"
  reconociendo el hueco.
- **(III) Enforcement solo cliente, sin documentar.** Pretender que el
  split de roles es real. Rechazada — mal hábito, y cualquier revisor que
  conozca Firebase lo verá a través.

### Decisión

Se adopta **(I) Firestore Security Rules** como fuente de verdad para
autorización. La UI cliente verifica roles solo por ergonomía.

### Reglas (resumen)

- `users/{uid}`
  - lectura: `request.auth.uid == uid` O el caller es admin
  - escritura: `request.auth.uid == uid` Y `request.resource.data.role
    == resource.data.role` (no puede cambiar su propio rol); los admins
    pueden escribir a cualquier usuario
- `suppliers/{id}`
  - lectura: cualquier usuario autenticado
  - escritura: solo admin
- `purchases/{id}`
  - lectura: cualquier usuario autenticado
  - creación: cualquier usuario autenticado, `createdBy ==
    request.auth.uid`
  - update/delete: admin, O (`createdBy == request.auth.uid` Y
    `request.time - resource.data.serverWrittenAt < duration.value(24, 'h')`)

> **Nota:** la ventana de 24h se mide contra `serverWrittenAt`, no contra
> el `enteredAt` establecido por el cliente. Esto es deliberado — ver
> glosario → "Tres timestamps, tres propósitos." Las escrituras encoladas
> offline obtienen una ventana de edición de 24h que comienza cuando el
> servidor acepta la escritura, no cuando el Operador pulsó guardar en el
> dispositivo.

### Consecuencias

**Positivas**

- La autorización es real, no teatral. El split admin/operator sobrevive
  un cliente hostil.
- El auto-ascenso es estructuralmente imposible, no solo oculto.
- Defendible en el examen oral / documento de arquitectura como un
  mecanismo concreto de seguridad server-side en lugar de una afordancia
  de UI.

**Negativas**

- ~1–2 horas de escritura de reglas y testing con emulador, sobre un
  presupuesto ajustado.
- Las reglas agregan un segundo lugar donde vive la lógica de
  autorización (el otro siendo el gating de UI client-side). Los dos
  deben mantenerse consistentes.

### Disparadores para revisitar

- Si el modelo de roles crece más allá de admin/operator (ej. admins con
  scope por bodega), las reglas necesitarán restructurarse y posiblemente
  custom claims.
- ~~Si la semántica de escrituras offline entra en conflicto con la
  ventana de edición de 24h del Operador (ej. una compra encolada
  offline por 30h y luego sincronizada)~~ *Resuelto 2026-05-26:* la
  ventana está anclada en `serverWrittenAt` (timestamp del servidor), no
  en el `enteredAt` establecido por el cliente.

---

> **Nota sobre la fuente de verdad:** los ADRs canónicos en inglés viven
> en `docs/Programa1/adr/`. Si se actualizan, esta traducción debe
> re-sincronizarse antes de la entrega final.
