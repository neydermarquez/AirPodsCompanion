# Matriz de validación física

No se marca una capacidad como compatible hasta observarla de forma reproducible en hardware real.

## Modelos

- AirPods 1, 2, 3 y 4.
- AirPods 4 con ANC.
- AirPods Pro 1.
- AirPods Pro 2 Lightning y USB-C.
- AirPods Max Lightning y USB-C.

## Teléfono de prueba

Registrar por ejecución:

- Fabricante y modelo.
- Versión de Android.
- Modelo y nombre Bluetooth de los AirPods.
- Versión de firmware, solo cuando una fuente fiable la publique.

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

- Capturar únicamente señales obtenidas legalmente mediante APIs públicas.
- Repetir cada transición al menos tres veces.
- Comparar con un estado de control.
- No añadir un decodificador si el cambio no es estable o depende de bytes desconocidos.
