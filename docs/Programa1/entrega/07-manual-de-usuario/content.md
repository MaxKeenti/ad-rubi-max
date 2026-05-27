# Manual de Usuario

> **NOTA:** este documento se finaliza cuando la app esté implementada
> (capturas de pantalla reales reemplazarán los placeholders
> `<!-- SCREENSHOT: ... -->`). La estructura y los textos están listos
> para usarse como guion de redacción.

## 1. Instalación

### Requisitos del dispositivo

- Android 8.0 (API 26) o superior.
- Conexión a internet **solo para el primer inicio de sesión** y para
  sincronizar las capturas. Las operaciones cotidianas funcionan offline.
- Espacio libre: ~50 MB.

### Instalar el APK

1. Recibirás un archivo `mangos-v1.0.apk` por correo o vía un enlace de
   descarga.
2. En el teléfono Android, abre el archivo descargado.
3. Si Android te avisa "instalar de fuentes desconocidas", acepta. Es
   normal en distribución privada (no estamos publicados en Play Store).
4. Confirma la instalación. El icono **Mangos** aparecerá en el cajón de
   apps.

<!-- SCREENSHOT: instalación-1.png, instalación-2.png -->

## 2. Iniciar sesión

Tu Administrador te entrega un correo y una contraseña iniciales. La
primera vez que abres la app:

1. Pulsa el icono **Mangos**.
2. Captura tu correo y contraseña.
3. Pulsa **Iniciar sesión**.

<!-- SCREENSHOT: login.png -->

> **No hay opción de "Crear cuenta"** en esta versión. Si no tienes
> credenciales, contacta a tu Administrador.

## 3. Dashboard

Esta es la pantalla principal. Muestra:

- **Toneladas registradas hoy** — suma de todas las compras del día.
- **Número de compras hoy** — cuántas entradas se han capturado.
- **Proveedores activos** — cuántos proveedores están disponibles para
  capturar.
- **Últimas 5 compras** — lista breve para revisar lo recién capturado.
- **Botón flotante (+)** — atajo rápido para registrar una nueva
  entrada.

<!-- SCREENSHOT: dashboard.png -->

### Indicador "Pendiente"

Si una compra muestra un pequeño icono o etiqueta **"pendiente"**,
significa que se capturó offline y aún no se ha sincronizado con el
servidor. La compra está guardada localmente y no se va a perder; en
cuanto el teléfono recupere conectividad, se sincroniza automáticamente
y el indicador desaparece.

## 4. Registrar una compra (entrada de camión)

Esta es **la operación principal** que harás varias veces al día.

1. En el Dashboard, pulsa el botón flotante **(+)**.
2. **Selecciona el proveedor** desde el dropdown.
   - Si el proveedor no aparece en la lista, selecciona **"Proveedor no
     registrado"** y captura el nombre real en el campo de notas que
     aparecerá.
3. **Captura las toneladas** entregadas.
4. **(Opcional) Captura el precio por tonelada** en MXN. Si aún no
   tienes el precio, déjalo en blanco — el Administrador podrá llenarlo
   después.
5. **Confirma la fecha** (por defecto, la fecha de hoy). Cambia la fecha
   si estás capturando una entrega de un día anterior.
6. Pulsa **Guardar**.

La compra aparece inmediatamente en el Dashboard y en el historial,
incluso si estás offline.

<!-- SCREENSHOT: add-purchase.png, add-purchase-unregistered.png -->

### Corregir un typo

Si te das cuenta que capturaste mal una compra:

- **Operador:** puedes editar o borrar tu propia compra **dentro de las
  primeras 24 horas** después de que el servidor la haya recibido. Si
  capturaste offline, la ventana de 24h empieza cuando se sincroniza.
- **Administrador:** puede editar o borrar cualquier compra, sin límite
  de tiempo.

Las compras borradas no se eliminan físicamente — se ocultan de la lista
pero quedan en el sistema para auditoría.

## 5. Historial de compras

Pestaña **Compras** en la barra inferior.

- Lista todas las compras vivas (no borradas), de la más reciente a la
  más antigua.
- **Filtro por proveedor:** pulsa el chip de proveedor para ver solo
  las compras de uno específico.

<!-- SCREENSHOT: history.png -->

## 6. Proveedores (solo Administrador)

> Esta pestaña solo aparece si tu rol es **Administrador**.

- **Lista** de todos los proveedores con su nombre, ubicación y variedad
  de mango.
- **Crear** un nuevo proveedor con el botón flotante.
- **Editar** un proveedor pulsando sobre él.
- **Desactivar** un proveedor deslizando hacia un lado. Un proveedor
  desactivado deja de aparecer en el dropdown del muelle pero las
  compras históricas siguen mostrando su nombre correctamente.

<!-- SCREENSHOT: suppliers.png, edit-supplier.png -->

## 7. Reportes

Pestaña **Reportes**.

- **Toneladas de hoy** — número grande, suma del día.
- **Gasto de hoy en MXN** — calculado solo sobre compras con precio
  capturado. Si hay compras sin precio, se indica "(N entradas sin
  precio)" debajo del número.
- **Top 5 proveedores del mes** — lista textual de los proveedores con
  más toneladas en el mes en curso.

<!-- SCREENSHOT: reports.png -->

## 8. Cerrar sesión

1. En el Dashboard, pulsa el icono de menú **(⋮)** en la esquina
   superior derecha.
2. Selecciona **Cerrar sesión**.

<!-- SCREENSHOT: logout.png -->

## 9. Preguntas frecuentes

**¿Qué pasa si capturo una entrada sin internet?**
Se guarda localmente y se muestra con un indicador "pendiente". Cuando
el teléfono recupera conectividad, se sincroniza automáticamente. No
necesitas hacer nada.

**Capturé mal una compra hace más de 24 horas. ¿Cómo la corrijo?**
Pídele al Administrador que la edite o borre. El Administrador no tiene
límite de tiempo.

**Mi compañero ve "pendiente" en una de mis compras. ¿Hay un problema?**
No necesariamente. Significa que tu captura aún no llegó al servidor
(probablemente capturaste offline). Mientras tu teléfono no se conecte,
otros usuarios no la verán.

**Selecccioné "Proveedor no registrado" para un camión. ¿Y ahora?**
Continúa con tu día. El Administrador revisa periódicamente las compras
con proveedor no registrado, da de alta al proveedor real, y actualiza
la compra para apuntar al nuevo proveedor.

**Olvidé mi contraseña.**
Pídele al Administrador que la resetee desde la Consola de Firebase.
