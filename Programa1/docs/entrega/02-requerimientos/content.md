# Requerimientos

## 1. Contexto del problema

Ver el resumen ejecutivo (entregable 01) para la descripción del problema
de negocio. Este documento captura los requerimientos derivados de ese
contexto.

## 2. Actores

- **Operador.** Trabajador de bodega responsable de registrar las entradas
  de camiones en el muelle. Cada Operador tiene su propio teléfono y su
  propia cuenta (ver ADR-0001).
- **Administrador.** Gestiona el catálogo de proveedores, el roster de
  Operadores, la creación de cuentas y la reconciliación de proveedores no
  registrados; además puede editar o eliminar (suave) cualquier compra.
  También puede registrar compras como un Operador.

No existe un rol intermedio. El primer Administrador se crea manualmente en
Firebase para bootstrap. Después no hay auto-registro: ni los Operadores ni
los Administradores pueden darse de alta a sí mismos; los crea otro
Administrador autenticado desde la app.

## 3. Requerimientos funcionales

### 3.1 Autenticación (RF-AUTH)

| ID | Descripción | Prioridad |
|---|---|---|
| RF-AUTH-01 | El usuario inicia sesión con correo electrónico y contraseña. | Alta |
| RF-AUTH-02 | La sesión persiste entre aperturas de la app hasta que el usuario cierra sesión explícitamente. | Alta |
| RF-AUTH-03 | El usuario puede cerrar sesión desde el menú overflow del Dashboard. | Alta |
| RF-AUTH-04 | **No existe** pantalla de auto-registro. Ningún Operador ni Administrador puede registrar su propia cuenta. | Alta |

### 3.1.1 Gestión de usuarios (RF-USR)

| ID | Descripción | Prioridad |
|---|---|---|
| RF-USR-01 | El Administrador autenticado puede registrar nuevos Operadores desde una UI administrativa dentro de la app. | Alta |
| RF-USR-02 | El Administrador autenticado puede consultar, editar y administrar el roster de Operadores desde la app. | Alta |
| RF-USR-03 | El Administrador autenticado puede registrar otro Administrador desde la app. | Media |
| RF-USR-04 | Para registrar otro Administrador, el Administrador actuante debe volver a introducir sus credenciales de inicio de sesión como confirmación antes de enviar el alta. | Alta |
| RF-USR-05 | Un usuario no puede crear ni promover su propia cuenta; todas las altas de usuario ocurren por acción de otro Administrador autenticado. | Alta |
| RF-USR-06 | El Administrador autenticado puede promover un Operador seleccionándolo desde el roster de Operadores. | Media |
| RF-USR-07 | Al promover un Operador, el sistema desactiva la cuenta de Operador y crea una nueva cuenta de Administrador usando el mismo correo electrónico. | Alta |
| RF-USR-08 | Para completar la promoción, el Operador promovido debe volver a introducir su contraseña de inicio de sesión. | Alta |
| RF-USR-09 | Para completar la promoción, el Administrador actuante debe volver a introducir sus credenciales de inicio de sesión como confirmación final. | Alta |
| RF-USR-10 | Las compras históricas conservan el `uid` original del Operador promovido para no romper la atribución de auditoría. | Alta |

### 3.2 Registro de compras (RF-COMPRA)

| ID | Descripción | Prioridad |
|---|---|---|
| RF-COMPRA-01 | El Operador registra una compra seleccionando proveedor, cantidad en toneladas (obligatorio, > 0), precio por tonelada (opcional, en MXN), y fecha de recepción. | Alta |
| RF-COMPRA-02 | Si el proveedor no está en el catálogo, el Operador selecciona "Proveedor no registrado" y escribe el nombre real en un campo de notas libre. | Alta |
| RF-COMPRA-03 | La compra puede crearse **sin conexión a internet**; se sincroniza automáticamente cuando vuelve la red. | Alta |
| RF-COMPRA-04 | El Operador puede editar o eliminar (suave) una compra propia dentro de las primeras 24 horas posteriores a que el servidor la haya recibido. | Alta |
| RF-COMPRA-05 | El Administrador puede editar o eliminar (suave) cualquier compra en cualquier momento. | Alta |
| RF-COMPRA-06 | La eliminación de compras es siempre suave (`deletedAt`), nunca física. Los registros eliminados se filtran de todas las consultas por defecto. | Alta |
| RF-COMPRA-07 | El historial de compras muestra las compras vivas, ordenadas de la más reciente a la más antigua, con filtro por proveedor. | Media |

### 3.3 Catálogo de proveedores (RF-PROV)

| ID | Descripción | Prioridad |
|---|---|---|
| RF-PROV-01 | Solo el Administrador puede crear, editar o desactivar proveedores. | Alta |
| RF-PROV-02 | La desactivación de un proveedor es reversible (`isActive = false`); el proveedor desaparece del dropdown de captura pero permanece accesible para resolver compras históricas. | Alta |
| RF-PROV-03 | Existe permanentemente un proveedor reservado con id `UNREGISTERED` ("Proveedor no registrado") que sirve de comodín para entradas de camiones de proveedores aún no dados de alta. | Alta |
| RF-PROV-04 | Un proveedor tiene los campos: nombre, teléfono, correo, ubicación, variedad de mango, estado activo. | Alta |

### 3.4 Dashboard (RF-DASH)

| ID | Descripción | Prioridad |
|---|---|---|
| RF-DASH-01 | El Dashboard muestra: total de toneladas registradas hoy, número de compras hoy, número de proveedores activos. | Alta |
| RF-DASH-02 | El Dashboard muestra las últimas 5 compras con proveedor, cantidad y hora de captura. | Alta |
| RF-DASH-03 | Cada compra pendiente de sincronizar muestra un indicador visual de "pending". | Alta |
| RF-DASH-04 | El FAB del Dashboard abre directamente el formulario de nueva compra. | Alta |

### 3.5 Reportes (RF-REP)

| ID | Descripción | Prioridad |
|---|---|---|
| RF-REP-01 | Reporte de toneladas totales del día (excluyendo compras eliminadas). | Alta |
| RF-REP-02 | Reporte de gasto total del día en MXN, calculado solo sobre compras con precio capturado; muestra cuántas compras carecen de precio. | Alta |
| RF-REP-03 | Reporte de top 5 proveedores del mes por toneladas. Excluye el proveedor `UNREGISTERED`. | Media |
| RF-REP-04 | Los reportes consultan por `dateKey` (día calendario en zona horaria de México), no por rangos UTC. | Alta |

## 4. Requerimientos no funcionales

### 4.1 Plataforma y stack

- **Android** ≥ API 26 (Android 8.0) — el proyecto actual define
  `minSdk = 26`, `targetSdk = 36` y `compileSdk = 36`.
- **Kotlin** + Jetpack Compose + Material 3.
- **Backend:** Firebase Firestore + Firebase Authentication; Cloud
  Functions para operaciones privilegiadas de usuarios cuando el proyecto
  Firebase está en plan Blaze, con fallback Spark protegido por reglas.

### 4.2 Disponibilidad y red

- La app debe ser **funcionalmente completa offline** para el flujo de
  captura de compras. Solo el primer inicio de sesión y la primera carga
  del catálogo de proveedores requieren conectividad.
- El indicador de sincronización pendiente debe ser visible al usuario
  para que distinga entre "guardado local" y "confirmado en servidor".

### 4.3 Seguridad

- La autorización es **servidor-side** vía Firestore Security Rules.
  Ningún rol o permiso se considera real si solo está implementado en la
  UI cliente. Ver ADR-0002.
- Los usuarios no pueden ascender su propio rol; las reglas lo prohíben
  estructuralmente.

### 4.4 Datos y precisión

- El dinero se almacena como `Long` de centavos (MXN). Nunca como
  `Double`. Firestore almacena números como IEEE 754; cents-as-integer es
  la única forma de garantizar precisión y agregación exacta.
- Las cantidades en toneladas pueden usar `Double` (precisión más que
  suficiente para los rangos de negocio).

### 4.5 Internacionalización

- UI completamente en **español**. Ningún string visible en inglés.
- Nombres de campo en código en **inglés** (convención de la industria);
  esto no contradice 4.5 porque no son visibles al usuario.

### 4.6 Mantenibilidad

- Arquitectura por capas (MVVM + Clean) con repositorios que abstraen
  Firebase. Esto permite reemplazar el backend en el futuro sin tocar la
  capa de UI ni los ViewModels.
- Inyección de dependencias con **Hilt**.

## 5. Restricciones y supuestos explícitos

- **Un Operador, un teléfono.** Si en una iteración posterior se confirma
  que la operación usa tabletas compartidas en el muelle, se aplicará el
  modelo descrito en ADR-0001 (sección "Trigger to revisit").
- **Una sola región/bodega.** El diseño es forward-compatible con
  multi-bodega (el campo `dateKey` ya se calcula en zona local), pero v1
  asume Ciudad de México (UTC−6 sin horario de verano).
- **Una sola moneda.** MXN. Sin override por compra. Ver sección
  "Money" del glosario.
- **Ex-empleados fuera de promoción.** La baja general de empleados sigue
  siendo operación administrativa en Firebase Auth. La app sí modela
  retiro/desactivación para el caso específico de promoción de Operador a
  Administrador (`disabledAt`, `retiredAt`, `promotedToUid`).
