# Trazabilidad funcional

Este documento cruza el inventario del proyecto con la implementación. Ninguna capacidad propietaria se considera funcional sin evidencia reproducible obtenida con hardware real.

## Conexión y estado

| Requisito | Implementación | Estado |
|---|---|---|
| Detección y reconexión | Bluetooth Classic, dispositivos vinculados, A2DP/HFP y reconexión supervisada | Implementado |
| Emparejamiento | Solicitud pública `createBond` y continuación en Android | Implementado |
| Modelo | Identificación conservadora a partir del nombre publicado | Implementado con limitaciones |
| Conexión y desconexión | Perfiles A2DP/HFP, historial, ventana y notificaciones | Implementado |
| Auriculares y estuche | Lecturas separadas solo cuando existe señal confirmada | Implementado con evidencia |
| Estado de carga | Visible únicamente cuando la señal lo publica | Implementado con evidencia |
| Batería general | Eventos HFP públicos y caducidad de lectura | Implementado |
| Último estado | Snapshot local, historial y widget | Implementado |
| Errores | Centro de errores e historial persistente | Implementado |
| Bajo consumo | Se muestra como no disponible para control desde Android | Documentado |

## Audio y llamadas

| Requisito | Implementación | Estado |
|---|---|---|
| Reproducir, pausar, anterior y siguiente | Eventos multimedia públicos | Implementado |
| Volumen | `AudioManager` | Implementado |
| Salida multimedia | Diagnóstico de rutas de audio | Implementado |
| Micrófono | Detección de entrada Bluetooth | Implementado |
| Silenciar/activar | Acción disponible durante llamada y con micrófono Bluetooth | Implementado |
| Pantalla bloqueada | Gestionada por MediaSession/reproductor de Android | Gestionado por Android |
| Códec | La API pública no entrega el códec activo a esta app | No publicado |
| Latencia | Requiere medición física de reproducción/captura | Pendiente de hardware |

## Sensores, gestos y modos

Todas estas funciones aparecen individualmente en Dispositivos con uno de seis estados: controlable, detectable, control físico, gestionado por Android, no disponible o pendiente de evidencia.

- Doble toque.
- Sensor de presión.
- Digital Crown.
- Botón de modo de escucha.
- Personalización de acciones.
- Detección de oído.
- Pausa y reanudación automáticas.
- ANC.
- Transparencia.
- Audio adaptativo.
- Conciencia de conversación.
- Volumen personalizado.
- Ecualización adaptativa.
- Reducción de sonidos fuertes.
- Gestos de cabeza para llamadas y notificaciones.

Los estados detectables solo se elevan cuando el laboratorio acumula comparaciones reproducibles.

## Audio espacial

| Requisito | Implementación | Estado |
|---|---|---|
| Audio espacial de Android | `Spatializer` público | Implementado |
| Seguimiento de cabeza | Estado publicado por `Spatializer` | Implementado |
| Personalización Apple | No existe API Android | No disponible |

## Experiencia y plataforma

| Requisito | Implementación | Estado |
|---|---|---|
| Ventana de conexión | Estados conectado, reconectando y perdido | Implementado |
| Widget | Automático, compacto y ampliado | Implementado |
| Notificaciones | Conexión, desconexión, batería y reconexión | Implementado |
| Información por modelo | Modelo, confianza, batería, audio y 41 capacidades | Implementado |
| Preferencias | Perfil, avisos, supervisión, retención y privacidad | Implementado |
| Nombre y vínculo | Acceso directo a Ajustes Bluetooth, propietario del dato | Implementado |
| Accesibilidad | Semántica, áreas táctiles, texto ampliado y anuncios | Implementado |
| Privacidad | Procesamiento local, exportación y borrado | Implementado |

## Servicios Apple sin API Android

Se muestran explícitamente como no disponibles:

- Siri.
- iCloud.
- Cambio automático del ecosistema Apple.
- Red Buscar.
- Actualización de firmware.
- Audio espacial personalizado.
- Salud auditiva, prueba de audición, audífono y protección avanzada.
- Apple Intelligence y traducción propietaria.

## Validación pendiente de hardware

- Confirmar batería por generación y teléfono.
- Confirmar señales de carga.
- Medir latencia física.
- Verificar micrófono durante llamadas reales.
- Comparar capturas de ANC, Transparencia, Adaptativo, oído y gestos.
- Elevar una capacidad de “pendiente” a “detectable” únicamente con tres o más muestras reproducibles.
