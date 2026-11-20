# Hoja de ruta competitiva para CRM turístico

Esta hoja de ruta convierte el análisis de brechas del producto en entregas verificables para agencias de viajes colombianas. Una opción no se considera terminada solo por tener una pantalla: debe usar datos reales, respetar el tenant, contar con autorización, pruebas y un flujo operable.

## Fase 1 — Inteligencia comercial turística (en ejecución)

- Cliente 360 basado en ventas Zeus: tarifa, frecuencia, recencia, ticket promedio, aerolínea preferida y última compra.
- Segmentos automáticos: VIP, frecuente, crecimiento, recuperar y nutrir.
- Puntajes explicables de potencial, interacción y riesgo de pérdida.
- Próxima mejor acción comercial y alertas de concentración por aerolínea.
- Filtros por periodo con detección del rango real disponible.

## Fase 2 — Operación y reservas

- Expediente único de reserva: localizador, pasajeros, tramos, fechas, tarifa, reglas, cambios y anulaciones.
- Línea de tiempo del viajero y documentos asociados.
- Control de comisiones, fees, incentivos y rentabilidad por venta.
- Catálogo normalizado de aerolíneas, hoteles, operadores y mayoristas.
- Tareas y alertas por emisión, vencimiento, viaje próximo y posventa.

## Fase 3 — Cartera y finanzas

- Saldo por cliente, antigüedad de cartera y cupo de crédito.
- Conciliación entre venta, factura, recaudo y devolución.
- Alertas de vencimiento y flujo de cobranza aprobado por humanos.
- Rentabilidad por cliente, ruta, aerolínea, oficina y asesor.

## Fase 4 — Automatización de Zeus

- Prioridad: API oficial o acceso documentado a la base de Zeus Agencias.
- Alternativa controlada: agente local que ejecuta Fare Basis y deposita el archivo en una carpeta vigilada.
- Importación incremental con cursor, deduplicación, reintentos, auditoría y conciliación.
- Panel de salud de sincronización: última ejecución, filas procesadas, duplicados y errores.

## Fase 5 — Distribución aérea y NDC

- Conectores oficiales para Amadeus y NDC seleccionados.
- Búsqueda y comparación de ofertas, familias tarifarias y servicios complementarios.
- Seguimiento de órdenes, cambios, reembolsos y estados de ticket.
- Contratos, credenciales y límites gestionados en un almacén seguro; nunca en el frontend.

## Fase 6 — Omnicanal y fidelización

- WhatsApp Cloud API, correo y webchat en una bandeja compartida.
- Consentimiento, plantillas aprobadas, entrega y trazabilidad de conversaciones.
- Campañas por segmento con atribución a ventas y medición de retorno.
- Encuestas posviaje, NPS y recuperación de clientes insatisfechos.

## Fase 7 — Datos y operación productiva

- PostgreSQL, copias verificadas, restauración, observabilidad y objetivos de servicio.
- Métricas con moneda, zona horaria, fuente y periodo explícitos.
- Cohortes, LTV, retención, precisión de pronósticos y comparación contra periodos anteriores.
- Evaluación de modelos, deriva, aprobación humana y auditoría de acciones sensibles.

## Orden recomendado de las próximas entregas

1. Expediente de reserva y rentabilidad.
2. Cartera y conciliación.
3. Sincronización incremental Zeus.
4. Alertas/tareas automáticas.
5. Primer conector oficial NDC.
6. Omnicanal y fidelización.
7. Migración y controles de producción.
