# Trazabilidad funcional

Este documento cruza el inventario del proyecto con la implementación. Ninguna capacidad propietaria se considera funcional sin evidencia reproducible obtenida con hardware real.

## Base publicada

La referencia documental actual es `v1.0.2` (`versionCode` 3, commit de compilación `6c9dc70`). Esta versión incorpora por defecto un puente JNI experimental, compilado para las cuatro ABI admitidas. El puente amplía intentos de diagnóstico, pero no cambia el criterio de evidencia ni garantiza acceso en todos los fabricantes o versiones de Android.

## Conexión y estado

| Requisito | Implementación | Estado |
|---|---|---|
| Detección y reconexión | Bluetooth Classic, nombres publicados tardíamente, dispositivos conocidos renombrados, A2DP/HFP y reconexión supervisada | Implementado |
| Emparejamiento | Solicitud pública `createBond`, intento de perfiles de audio y acceso guiado a Bluetooth del sistema cuando Android bloquea la conexión directa | Implementado |
| Modelo | Identificación conservadora a partir del nombre publicado | Implementado con limitaciones |
| Conexión y desconexión | Perfiles A2DP/HFP, historial, ventana y notificaciones | Implementado |
| Auriculares y estuche | Modelo de datos, interfaz, persistencia y fusión sin sobrescritura; adquisición solo si llega una señal observable permitida | Presentación implementada; adquisición dependiente del teléfono/modelo |
| Estado de carga | Visible únicamente cuando una señal observable lo publica | Presentación implementada; adquisición dependiente del teléfono/modelo |
| Batería general | Eventos HFP, evento de batería de Android, consulta de respaldo cada 15 s, caducidad y deduplicación | Implementado |
| Último estado | Snapshot local, historial y widget | Implementado |
| Errores | Centro de errores e historial persistente | Implementado |
| Bajo consumo | Se muestra como no disponible para control desde Android | Documentado |

## Audio y llamadas

| Requisito | Implementación | Estado |
|---|---|---|
| Reproducir, pausar, anterior y siguiente | Delegado a la sesión del reproductor activo para evitar controles duplicados | Gestionado por el reproductor |
| Volumen | `AudioManager` | Implementado |
| Salida multimedia | Diagnóstico de rutas de audio | Implementado |
| Micrófono | Detección de entrada Bluetooth | Implementado |
| Silenciar/activar | Acción disponible durante llamada y con micrófono Bluetooth | Implementado |
| Pantalla bloqueada | Gestionada por MediaSession/reproductor de Android | Gestionado por Android |
| Códec | Lectura de `BluetoothCodecStatus` con firma por dispositivo y compatibilidad con firmas antiguas mediante JNI/reflexión | Dependiente del sistema |
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
| Notificaciones | Conexión, desconexión, batería y reconexión; la notificación persistente consume el mismo estado actual del repositorio | Implementado |
| Información por modelo | Modelo, confianza, batería, audio y 41 capacidades | Implementado |
| Preferencias | Perfil, volumen y avisos aplicados al seleccionar; supervisión, retención y privacidad | Implementado |
| Nombre y vínculo | Administración interna, alias local y aplicación del alias del sistema cuando Companion Device Manager lo autoriza | Implementado con degradación segura |
| Accesibilidad | Semántica, áreas táctiles, texto ampliado y anuncios | Implementado |
| Privacidad | Procesamiento local, exportación y borrado | Implementado |
| Acceso a Buscar | Pestaña segura hacia `iCloud.com/find`, sin lectura de credenciales, cookies ni resultados | Implementado como acceso externo |

## Servicios Apple sin API Android

Se muestran explícitamente como no disponibles:

- Siri.
- Datos y APIs internas de iCloud.
- Cambio automático del ecosistema Apple.
- Lectura o control directo de la red Buscar.
- Actualización de firmware.
- Audio espacial personalizado.
- Salud auditiva, prueba de audición, audífono y protección avanzada.
- Apple Intelligence y traducción propietaria.

La aplicación puede abrir el sitio oficial de Buscar dentro de una pestaña segura administrada por el navegador. Este acceso no cambia el estado de las APIs: AirPods Companion no controla ni recibe datos de Buscar.

## Validación pendiente de hardware

- Ejecutar las capturas funcionales principales desde el laboratorio integrado, sin depender de ADB.
- Verificar qué teléfonos publican el evento general de batería y con qué latencia.
- Comprobar si alguna implementación de fabricante expone señales observables separadas sin `BLUETOOTH_PRIVILEGED`.
- Confirmar en teléfono físico qué llamadas del puente nativo siguen permitidas por fabricante y versión de Android.
- Confirmar batería por generación y teléfono.
- Confirmar señales de carga.
- Medir latencia física.
- Verificar micrófono durante llamadas reales.
- Comparar capturas de ANC, Transparencia, Adaptativo, oído y gestos.
- Elevar una capacidad de “pendiente” a “detectable” únicamente con tres o más muestras reproducibles.
