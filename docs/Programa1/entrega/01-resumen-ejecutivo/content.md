# Resumen Ejecutivo

## El problema

**Mangos USA** es una corporación dedicada a la compra y procesamiento de
mango. Actualmente, el registro diario de compras en bodega se realiza con
un **pizarrón físico y notas adhesivas**: cada vez que llega un camión de
un proveedor, el operador anota el peso entregado en una nota y la pega en
el pizarrón. Al final del día, alguien transcribe esa información a
hojas de cálculo.

Este flujo presenta varios problemas:

- **Pérdida de información** cuando una nota se cae o se borra.
- **Sin trazabilidad de quién registró qué** ni cuándo se modificó.
- **Sin métricas agregadas en tiempo real** (¿cuántas toneladas llevamos
  hoy? ¿qué proveedor lidera la semana?).
- **Sin respaldo histórico** consultable más allá del día en curso.

## La solución propuesta

Aplicación Android nativa que **reemplaza el pizarrón** preservando la
ergonomía del flujo actual:

- El operador registra cada entrada de camión desde su teléfono, **en el
  muelle, en el momento en que se pesa la carga**.
- Funciona **offline** — la conectividad en bodega es poco confiable; las
  capturas se sincronizan automáticamente cuando vuelve la red.
- Roles diferenciados: **Operador** captura entradas; **Administrador**
  gestiona el catálogo de proveedores y reconcilia datos.
- Reportes diarios y mensuales accesibles desde la misma app.

## Alcance

| Incluido en v1 | Diferido |
|---|---|
| Captura de compras (entradas) | Pantalla de Configuración |
| Catálogo de proveedores (CRUD admin) | Gestión de usuarios desde UI Admin |
| Autenticación con roles | Gráficos en Reportes |
| Reportes textuales (toneladas, top 5 proveedores) | Filtros por rango de fechas |
| Sincronización offline | Multi-bodega / multi-región |
| Reglas de seguridad servidor-side | Modo tableta compartida |

## Decisiones arquitectónicas clave

1. **MVVM + Clean Architecture** sobre Kotlin/Jetpack Compose.
2. **Firebase Firestore** como backend (NoSQL, persistencia offline nativa).
3. **Autorización del lado del servidor** vía Firestore Security Rules — la
   UI oculta acciones administrativas por ergonomía, pero la verdad
   autoritativa son las reglas. Ver ADR-0002.
4. **Autenticación por dispositivo personal** (un usuario por teléfono) en
   lugar de tableta compartida. Ver ADR-0001.
5. **Tres marcas de tiempo distintas** por compra (fecha de recepción,
   captura en cliente, escritura en servidor) para reconciliar correctamente
   las escrituras offline con la ventana de edición de 24 horas.
6. **Dinero como `Long centavos`**, nunca como `Double` — Firestore
   almacena números como IEEE 754 y la agregación en Reportes acumula
   error de punto flotante.

## Equipo y plazos

- **Equipo:** González Calzada Maximiliano (2021601769), Sosa Montoya Melanie Rubí (2024601345).
- **Plazo de entrega:** 1 de junio de 2026.
- **Estimación de esfuerzo:** ≈20.5 horas de desarrollo + documentación,
  repartidas en 5 días.

## Resultados esperados

- APK instalable y demostrable en emulador o dispositivo físico.
- Documentación técnica completa en español (este conjunto de entregables).
- Repositorio Git con historial de commits que evidencia el proceso.
