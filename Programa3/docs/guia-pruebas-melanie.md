# Guía de pruebas manuales - BacheWatch

**Para:** Melanie
**Escribió:** Max
**Objetivo:** validar en teléfono real lo que los tests automáticos no
cubren: GPS real, cámara real, datos móviles, permisos, App Distribution
y evidencia visual para la entrega.

Guarda todas las capturas en `docs/screenshots/` con el nombre indicado
en la columna **Evidencia**. En **Resultado** marca `OK`, `Falla` o
`N/A`, y agrega notas cuando algo no coincida.

## 0. Datos de la corrida

| Campo | Valor |
|---|---|
| Fecha | |
| Telefono / Android | |
| Cuenta Google que acepto App Distribution | |
| Build instalado | |
| Red usada | Datos móviles / Wi-Fi |
| Zona de prueba | |

## 1. Instalacion por App Distribution

| # | Accion | Resultado esperado | Evidencia | Resultado / notas |
|---|---|---|---|---|
| 1 | Abre el correo o enlace de Firebase App Distribution en el teléfono. | El enlace abre Firebase App Distribution con BacheWatch disponible. | `01-app-distribution-invite.png` | |
| 2 | Acepta la invitación con la misma cuenta Google que usa Play Services en el teléfono. | La app queda disponible para descargar. Si se queda en "downloading", cambia a esa misma cuenta Google y reintenta. | `01-app-distribution-account.png` | |
| 3 | Instala y abre BacheWatch. | Aparece el mapa con título **BacheWatch** y acciones **Recientes**, **Reportar** y chip **Zonas**. | `01-home-mapa.png` | |

## 2. Flujo de reporte completo en calle

Prueba principal: hazla con datos móviles, parada junto a un bache real.
Anota la precisión bajo cielo abierto y entre edificios si puedes.

| # | Accion | Resultado esperado | Evidencia | Resultado / notas |
|---|---|---|---|---|
| 1 | En el mapa toca **Reportar**. | Abre la pantalla **Reportar bache**. | `02-reportar-bache.png` | |
| 2 | Si Android pide ubicación, elige permiso preciso. | La app puede obtener ubicación; si Android ofrece precisa/aproximada, debe quedar en precisa. | `02-permiso-preciso.png` | |
| 3 | Toca **Tomar foto** y fotografía el bache. | La cámara del sistema se abre y al aceptar vuelve una vista previa de la foto con **Volver a tomar**. | `02-foto-capturada.png` | |
| 4 | Espera la tarjeta de ubicación. | Debe mostrar **Ubicación capturada (±N m)**. Anota N bajo cielo abierto: `____ m`; entre edificios: `____ m`. | `02-ubicacion-capturada.png` | |
| 5 | En **Severidad (opcional)** toca **Leve**, luego **Moderado**, luego **Severo**. | Cada chip se selecciona al tocarlo; dejar uno elegido no bloquea el envio. | `02-severidad.png` | |
| 6 | En **Descripción (opcional)** escribe una nota corta del lugar. | El contador no rebasa `200/200`. | `02-descripcion.png` | |
| 7 | Con datos móviles activos toca **Enviar reporte**. | El botón muestra **Enviando…** y regresa al mapa. El nuevo marcador aparece en la zona del bache. | `02-marcador-nuevo.png` | |
| 8 | Abre **Recientes**. | El reporte nuevo aparece arriba con foto, severidad, descripción y contador de confirmaciones. | `02-recientes-nuevo.png` | |

## 3. Soft accuracy gate

Esta prueba fuerza un fix malo. La app debe advertir, no bloquear.

| # | Accion | Resultado esperado | Evidencia | Resultado / notas |
|---|---|---|---|---|
| 1 | Entra a un lugar techado o pegado a edificios altos y toca **Reportar**. | Abre **Reportar bache**. | `03-reportar-techado.png` | |
| 2 | Toca **Tomar foto** y acepta la foto. | La foto queda lista. | `03-foto-techado.png` | |
| 3 | Espera el fix. | Si la precisión es baja, aparece **Precisión baja — puedes guardar de todos modos o reintentar**. | `03-precision-baja.png` | |
| 4 | Toca **Enviar reporte** sin reintentar. | El reporte se guarda aunque la precisión sea baja. | `03-guardado-precision-baja.png` | |

## 4. Retry con modo avion

| # | Accion | Resultado esperado | Evidencia | Resultado / notas |
|---|---|---|---|---|
| 1 | Inicia un reporte normal hasta tener foto y ubicación capturadas. | **Enviar reporte** queda habilitado. | `04-listo-para-enviar.png` | |
| 2 | Activa modo avión antes de tocar **Enviar reporte**. | El teléfono queda sin red. | `04-modo-avion.png` | |
| 3 | Toca **Enviar reporte**. | La pantalla conserva foto, ubicación, severidad y descripción; aparece error y el botón cambia a **Reintentar**. | `04-error-reintentar.png` | |
| 4 | Desactiva modo avion y toca **Reintentar**. | El envio funciona y vuelve al mapa. | `04-reintento-ok.png` | |
| 5 | Revisa **Recientes**. | Solo aparece un reporte nuevo para esa prueba; no debe haber duplicados por el retry. | `04-sin-duplicados.png` | |

## 5. Permisos de ubicación

| # | Accion | Resultado esperado | Evidencia | Resultado / notas |
|---|---|---|---|---|
| 1 | Desde ajustes de Android borra el permiso de ubicación de BacheWatch y abre la app. | Mapa, **Zonas**, **Recientes** y detalle siguen funcionando sin pedir ubicación. | `05-readonly-sin-permiso.png` | |
| 2 | Toca **Reportar** y deniega ubicación. | La pantalla muestra **Se necesita ubicación precisa para reportar**, con **Conceder** y **Abrir ajustes**. | `05-denegar-ubicacion.png` | |
| 3 | En Android 12+ concede solo ubicación aproximada. | Para reportar se trata como denegada; no debe permitir enviar un reporte con ubicación aproximada. | `05-aproximada-denegada.png` | |
| 4 | Toca **Abrir ajustes**, concede ubicación precisa y vuelve a la app. | El flujo de reporte vuelve a poder obtener ubicación. | `05-ajustes-precisa.png` | |

## 6. Cancelar cámara

| # | Accion | Resultado esperado | Evidencia | Resultado / notas |
|---|---|---|---|---|
| 1 | Toca **Reportar** y luego **Tomar foto**. | Se abre la cámara del sistema. | `06-camara-abierta.png` | |
| 2 | Cancela la cámara con atrás/cancelar. | Regresa a **Reportar bache** sin error; **Tomar foto** sigue disponible. | `06-camara-cancelada.png` | |
| 3 | Toca **Volver**. | Regresa al mapa; la app sigue usable. | `06-volver-mapa.png` | |

## 7. Confirmar un reporte de Max

Usa un reporte que no hayas creado tú en esta instalación.

| # | Accion | Resultado esperado | Evidencia | Resultado / notas |
|---|---|---|---|---|
| 1 | En el mapa o en **Recientes**, abre un reporte de Max. | Se abre el detalle con foto, severidad, tiempo, descripción y `N confirmaciones`. | `07-detalle-max.png` | |
| 2 | Toca **Confirmar**. | El contador sube en 1 y el botón pasa a **Confirmado ✓** / bloqueado. | `07-confirmado.png` | |
| 3 | Cierra y vuelve a abrir el mismo reporte. | El contador se mantiene y el botón sigue bloqueado; no se puede confirmar dos veces. | `07-confirmado-persiste.png` | |

## 8. Eliminar propio menor a 24 h

| # | Accion | Resultado esperado | Evidencia | Resultado / notas |
|---|---|---|---|---|
| 1 | Crea un reporte nuevo desde este teléfono. | El reporte aparece en mapa y **Recientes**. | `08-reporte-propio.png` | |
| 2 | Abre el detalle del reporte propio. | Aparece el botón **Eliminar** porque es tuyo y tiene menos de 24 h. | `08-eliminar-visible.png` | |
| 3 | Toca **Eliminar**. | Aparece diálogo **Eliminar reporte** con **Eliminar** y **Cancelar**. | `08-dialogo-eliminar.png` | |
| 4 | Confirma **Eliminar**. | El detalle se cierra y el reporte desaparece del mapa. | `08-eliminado-mapa.png` | |
| 5 | Abre **Recientes**. | El reporte eliminado ya no aparece. | `08-eliminado-recientes.png` | |
| 6 | Abre un reporte ajeno. | No debe aparecer **Eliminar**. | `08-ajeno-sin-eliminar.png` | |

## 9. Heatmap Zonas

| # | Accion | Resultado esperado | Evidencia | Resultado / notas |
|---|---|---|---|---|
| 1 | En el mapa toca el chip **Zonas**. | Los marcadores se sustituyen por un heatmap. | `09-heatmap-zonas.png` | |
| 2 | Compara zonas con reportes severos y leves. | Las zonas con severidad **severo** deben verse con más peso/brillo que zonas solo **leve**. | `09-severo-mas-peso.png` | |
| 3 | Toca **Zonas** otra vez. | Regresan los marcadores y los reportes no desaparecen. | `09-marcadores-regresan.png` | |

## 10. Capturas para entrega

| # | Accion | Resultado esperado | Evidencia | Resultado / notas |
|---|---|---|---|---|
| 1 | Captura el mapa normal con varios marcadores. | Se ve **BacheWatch**, **Reportar**, **Recientes** y marcadores. | `10-mapa-marcadores.png` | |
| 2 | Captura el mapa con **Zonas** activo. | Se ve el heatmap. | `10-heatmap.png` | |
| 3 | Captura el flujo **Reportar bache** con foto y ubicación. | Se ven foto, ubicación, severidad, descripción y **Enviar reporte**. | `10-reportar-flow.png` | |
| 4 | Captura un detalle de reporte. | Se ven foto, severidad, confirmaciones y **Confirmar** o **Confirmado**. | `10-detalle-confirmar.png` | |
| 5 | Captura **Recientes**. | Se ve la lista con fotos y confirmaciones. | `10-recientes.png` | |
| 6 | Captura el diálogo de permiso de Android o la tarjeta sin permiso. | Queda evidencia de la ruta de permisos. | `10-permisos.png` | |

## Hoja de resultados

| Caso | Resultado | Notas |
|---|---|---|
| 1. Instalacion App Distribution | | |
| 2. Flujo de reporte completo | | |
| 3. Soft accuracy gate | | |
| 4. Retry modo avion | | |
| 5. Permisos ubicación | | |
| 6. Cancelar cámara | | |
| 7. Confirmar | | |
| 8. Eliminar propio | | |
| 9. Heatmap Zonas | | |
| 10. Capturas entrega | | |

## Hallazgos

Anota aqui fallas, diferencias contra los textos esperados, capturas que
faltan, o cualquier comportamiento raro:

-
