# Matriz de validación física

No se marca una capacidad como compatible hasta observarla de forma reproducible en hardware real.

## Versión de referencia

- Aplicación: `v1.0.1`.
- Commit usado para compilar los binarios: `f33707f`.
- Paquete: `com.soren.airpodscompanion`.
- Puente nativo: habilitado por defecto.
- NDK: `28.2.13676358`.
- CMake: `3.22.1`.

Antes de cada sesión, registrar también el SHA-256 del APK utilizado. La compilación de referencia incluye `libbluetooth_hidden_bridge.so` para `arm64-v8a`, `armeabi-v7a`, `x86` y `x86_64`.

## Estado de la matriz

La compilación, firma, instalación y apertura en emulador están verificadas. Las filas de hardware continúan pendientes hasta ejecutar pruebas con un teléfono Android físico y AirPods reales. La presencia del puente nativo no confirma por sí sola ninguna capacidad propietaria.

## Modelos

- AirPods 1, 2, 3 y 4.
- AirPods 4 con ANC.
- AirPods Pro 1.
- AirPods Pro 2 Lightning y USB-C.
- AirPods Max Lightning y USB-C.

## Teléfono de prueba

Registrar por ejecución:

- Fecha y persona que ejecutó la prueba.
- Versión de la aplicación, commit y SHA-256 del APK.
- Fabricante y modelo.
- Versión de Android.
- Modelo y nombre Bluetooth de los AirPods.
- Versión de firmware, solo cuando una fuente fiable la publique.
- Estado de permisos, Bluetooth y optimización de batería.

## Batería

Repetir cada escenario tres veces:

1. Ambos auriculares fuera del estuche.
2. Solo auricular izquierdo conectado.
3. Solo auricular derecho conectado.
4. Ambos auriculares dentro, estuche abierto.
5. Estuche conectado a carga.
6. Auricular izquierdo cargando.
7. Auricular derecho cargando.
8. Transición por debajo de 20 %.

Confirmar qué componentes publica Android y si el valor se repite después de desconectar y reconectar.

## Audio y llamadas

1. Reproducción, pausa, anterior y siguiente.
2. Subir y bajar volumen.
3. Salida A2DP.
4. Entrada de micrófono Bluetooth.
5. Silenciar y reactivar durante una llamada real.
6. Regreso correcto al audio multimedia al finalizar.

## Segundo plano

1. Salir de la aplicación.
2. Apagar y encender Bluetooth.
3. Conectar y desconectar los AirPods.
4. Verificar notificaciones e historial.
5. Verificar widget.
6. Mantener la app fuera de pantalla durante una hora y revisar consumo.

## Funciones propietarias

Para sensores de oído, ANC, Transparencia, audio adaptativo y audio espacial:

- Comenzar por APIs públicas y registrar por separado cualquier resultado del puente nativo experimental.
- No intentar evadir permisos, autenticación, cifrado, aislamiento del sistema ni restricciones del dispositivo.
- Repetir cada transición al menos tres veces.
- Comparar con un estado de control.
- No añadir un decodificador si el cambio no es estable o depende de bytes desconocidos.

## Registro mínimo por escenario

| Campo | Valor |
|---|---|
| Resultado | Compatible, limitado, no disponible o pendiente |
| Fuente | API pública, evento Bluetooth o puente nativo |
| Repeticiones | Mínimo 3 |
| Lectura actual | Valor observado |
| Lectura de control | Valor de comparación |
| Error o excepción | Mensaje anonimizado |
| Evidencia | Sesión local exportada con autorización |

Un resultado del puente nativo debe degradarse de forma segura a “no disponible” cuando Android o el fabricante bloqueen el método. Nunca debe mostrarse un valor estimado como lectura real.
