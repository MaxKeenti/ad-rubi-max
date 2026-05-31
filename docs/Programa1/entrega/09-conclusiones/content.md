# Conclusiones

## 1. Lo que se logró

Una aplicación Android nativa que digitaliza el flujo de captura de
compras de mango, manteniendo la ergonomía del pizarrón físico que
reemplaza:

- **Captura en el muelle, offline-first.** El operador puede registrar
  entradas de camiones sin conexión a internet; la sincronización es
  transparente cuando vuelve la red.
- **Autorización real, no decorativa.** Los roles admin/operator se
  enforzan del lado del servidor con Firestore Security Rules. Un
  Operador no puede ascenderse a Administrador, ni siquiera con acceso
  directo al SDK de Firestore.
- **Modelo de datos honesto sobre el tiempo y el dinero.** Tres
  timestamps distintos por compra reconcilian correctamente la captura
  offline con la ventana de edición de 24 horas. El dinero vive como
  `Long centavos` para evitar el error de punto flotante que Firestore
  acumularía con `Double`.
- **Arquitectura por capas.** UI / Domain / Data con repositorios que
  abstraen Firebase. Cambiar el backend en el futuro no requiere tocar
  ViewModels ni Composables.

## 2. Lo que se aprendió

Hablando como equipo, el proyecto consolidó varias lecciones que valen
más que el código entregable:

### El diseño se paga con tiempo de implementación

La sesión de grilling antes de codificar (ver
`grilling-session-2026-05-26.md` en el repo) duró un par de horas y
modificó decisiones que habrían costado días corregir mid-build:
soft-delete vs hard-delete, denormalización histórica vs back-fill,
ventana de edición anclada en servidor vs cliente, `dateKey` vs
matemática de timestamps UTC. **Discutir antes de teclear ahorra
reescrituras.**

### El servidor es la única frontera de seguridad real

Es tentador pensar que "ocultar la pestaña Proveedores para Operadores"
es suficiente. No lo es. Cualquiera con la APK puede ignorar la UI. La
única forma de tener un rol que signifique algo es expresarlo como una
regla que el servidor verifica. Esta es una lección que se traslada
directamente a cualquier sistema cliente-servidor, no solo Firebase.

### El offline cambia más cosas de las que parece

Decidir que "el Operador captura en el muelle, en el momento" sonó como
una decisión de UX. En realidad ramificó hacia:

- Las suppliers tienen que estar cacheadas localmente.
- Los Operadores no pueden crear proveedores nuevos sobre la marcha (de
  ahí el patrón `UNREGISTERED`).
- `createdAt` no puede ser un solo campo (de ahí las tres marcas de
  tiempo).
- La ventana de edición de 24h tiene que anclarse en el servidor, no en
  el cliente.
- Las consultas "compras de hoy" no pueden usar rangos UTC (de ahí
  `dateKey`).

Un solo requerimiento ergonómico ("captura en el muelle") definió la
estructura de la mitad del modelo de datos.

### Las cuotas y los presupuestos son herramientas de diseño

La estimación inicial era 16 horas. La estimación honesta post-grilling
era 22–26. El presupuesto real era ~30. Recortar Settings, Register,
gráficos y filtros de fecha **no fue una concesión** — fue lo que
permitió que las decisiones importantes (offline, autz servidor-side,
soft-delete) entraran sin sacrificar calidad. **Saber qué quitar es
parte del diseño.**

## 3. Limitaciones conscientes

Lo que **no** hace v1, por decisión explícita:

| Limitación | Razón | Disparador para revisitar |
|---|---|---|
| Sin pantalla de gestión de usuarios | En v1 el Admin provisiona cuentas en la Consola; resuelve el bootstrap del primer admin sin abrir auto-registro | Próxima iteración: UI Admin para registrar Operadores, gestionar Operadores, registrar otro Admin con re-autenticación y promover Operadores con doble confirmación |
| Sin tableta compartida en el muelle | Modelo de un teléfono por Operador (ADR-0001) | Si el cliente confirma "vamos a poner tabletas" |
| Sin gráficos en Reportes | Vico recortado por tiempo; texto es suficiente para v1 | Cuando termine el presupuesto y la app esté estable |
| Sin filtro por rango de fechas en Historial | Filtro por proveedor cubre el 80% de los casos | Si el Admin reporta que necesita comparar semanas/meses lado a lado |
| Una sola moneda (MXN) | Mangos USA opera en México | Si abre una bodega en EE.UU. |
| Una sola bodega/región | Diseño forward-compatible vía `dateKey` y `warehouseId` aditivo | Cuando abran segunda bodega |
| Sin UI para navegar compras eliminadas | Soft-delete sí; reverse-delete no | Si surge una incidencia que requiera "recuperar" una compra borrada por error |

## 4. Próximos pasos sugeridos

En orden de valor descendente:

1. **Pruebas de campo con operadores reales.** Toda la arquitectura
   asume un workflow ("el operador parado junto a la báscula"); falta
   verificar empíricamente que la UX coincide con cómo realmente
   trabajan.
2. **Gestión de usuarios desde la app.** Mantener cerrado el
   auto-registro, pero permitir que un Administrador autenticado registre
   Operadores, gestione el roster de Operadores y registre otro
   Administrador. El alta de otro Administrador debe pedir reingresar las
   credenciales del Admin actuante como confirmación. La promoción de
   Operador a Administrador debe retirar/desactivar la cuenta de Operador,
   crear una nueva cuenta Admin con el mismo correo y exigir confirmación
   tanto del Operador promovido como del Admin actuante.
3. **Gráficos en Reportes.** Es lo primero que un usuario va a pedir
   después de usar la app un mes. Vico se reincorpora aditivamente.
4. **Notificaciones push** para Administradores cuando hay compras con
   `supplierId == "UNREGISTERED"` pendientes de reconciliar.
5. **Modo tableta compartida (ADR-0001 opción ii).** Solo si el
   despliegue real lo justifica.
6. **Exportar a CSV/Excel** para integración con la contabilidad
   existente.
7. **Migración de `Long centavos` a `BigDecimal` serializado** si en
   algún punto se necesita precisión sub-centavo (no es esperable).

## 5. Reflexión del equipo

> **Espacio reservado.** Aquí va una sección breve y honesta sobre el
> proceso del equipo: cómo nos repartimos el trabajo, qué tan exacta
> resultó la estimación, qué fricciones técnicas tuvimos (Firebase
> setup, Hilt, Compose, etc.), qué haríamos diferente la próxima vez.
> Se redacta una vez que la implementación termine, no antes.
