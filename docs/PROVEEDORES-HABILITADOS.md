# Proveedores habilitados / deshabilitados

## Qué significa "deshabilitado"

Un proveedor deshabilitado conserva sus datos en la base de datos (canales, categorías, películas,
series y guía), pero la app no los lee ni abre conexiones hacia él:

- La guía, "todos los canales", búsqueda y favoritos filtran en SQL con
  `JOIN sources ... sources.enabled = 1`. No es un filtro solo de UI: los canales de un proveedor
  apagado no llegan al ViewModel.
- Las categorías (guía y manager web) salen de la misma unión, así que tampoco aparecen.
- La sincronización (`SyncWorker`, refresco manual, VOD y EPG) solo recorre
  `SourceRepository.enabled()`.
- El feed de guía de ese proveedor se oculta y no se descarga. Su fila se mantiene para que al
  reactivar el proveedor vuelva el mismo feed, sin tener que añadirlo otra vez.
- El selector del logo y los filtros de proveedor de la guía/Movies/Shows solo ofrecen proveedores
  habilitados. Si el proveedor seleccionado se apaga, la app vuelve a "todas las fuentes".
- Los detalles de VOD y los episodios no piden metadatos al proveedor apagado.

Al reactivarlo no hace falta reiniciar ni volver a sincronizar: los canales ya están en disco y las
consultas vuelven a incluirlos en cuanto cambia `sources.enabled`.

## Grabaciones ya programadas

Decisión: **deshabilitar un proveedor cancela sus grabaciones**.

- Las grabaciones futuras (`SCHEDULED`) de ese proveedor se desarman y quedan en la biblioteca como
  `FAILED` con el motivo "Proveedor deshabilitado". No se borran: el usuario ve qué se canceló y por
  qué.
- Una grabación en curso (`RECORDING`) de ese proveedor se detiene, para que "apagado" signifique de
  verdad cero tráfico.
- Al volver a habilitar el proveedor **no se reprograman automáticamente** las grabaciones
  canceladas. Hay que volver a programarlas de forma deliberada, igual que cualquier grabación
  perdida.

La alternativa (dejarlas correr) abriría conexiones contra un proveedor que el usuario acaba de
apagar y podría gastar una línea del proveedor sin que el usuario lo espere.
