# Captura HCI opcional para desarrollo

**Referencia revisada:** `v1.0.3`.

> Esta herramienta no forma parte del uso normal de AirPods Companion. Los usuarios y las pruebas funcionales ordinarias se realizan desde **Actividad > Laboratorio** usando únicamente el teléfono.

Esta guía obtiene evidencia reproducible sin root, sin reiniciar procesos del sistema y sin escribir paquetes Bluetooth desconocidos.

## Qué necesitamos

- Un teléfono Android físico con depuración USB autorizada.
- AirPods conectados y reproduciendo audio.
- Opciones de desarrollador activadas.
- `Registro de rastreo Bluetooth HCI` activado.
- Bluetooth apagado y encendido después de activar el registro.

Android recomienda activar el registro HCI desde Opciones de desarrollador y reiniciar Bluetooth. En teléfonos comerciales, la ruta `/data/misc/bluetooth/logs` normalmente no puede leerse directamente; por eso la herramienta genera un `bugreport`.

## Primera comprobación

En PowerShell:

```powershell
& "$env:LOCALAPPDATA\Android\Sdk\platform-tools\adb.exe" devices
```

El teléfono debe aparecer como `device`, no como `unauthorized`. El emulador no sirve para estas capturas.

## Ejecutar un escenario

Desde la raíz del proyecto:

```powershell
.\tools\capture-airpods.ps1 -Scenario anc
```

Escenarios admitidos:

- `anc`
- `transparency`
- `adaptive`
- `left-ear`
- `right-ear`
- `case-open`
- `charging`

La herramienta realiza tres repeticiones por defecto. En cada una:

1. Pide colocar los AirPods en un estado de control.
2. Inserta una marca temporal en `logcat`.
3. Pide ejecutar la transición.
4. Espera ocho segundos.
5. Inserta una marca de finalización.

Al terminar guarda:

- `capture-metadata.json`, sin número de serie.
- `logcat.txt`.
- Estado Bluetooth antes y después.
- Un `bugreport` que puede incluir el registro HCI.

Los archivos quedan bajo `captures/airpods/`, excluido de Git.

## Orden recomendado

No mezclar varias funciones en una misma ejecución:

```powershell
.\tools\capture-airpods.ps1 -Scenario anc
.\tools\capture-airpods.ps1 -Scenario transparency
.\tools\capture-airpods.ps1 -Scenario adaptive
```

Para batería y sensores:

```powershell
.\tools\capture-airpods.ps1 -Scenario left-ear
.\tools\capture-airpods.ps1 -Scenario right-ear
.\tools\capture-airpods.ps1 -Scenario case-open
.\tools\capture-airpods.ps1 -Scenario charging
```

## Reglas de análisis

- No asumir valores como `01`, `02` o `03`.
- No asumir que AirPods utiliza el servicio GATT Battery Service `0x180F`.
- No interpretar `ACTION_BATTERY_CHANGED` como batería de los AirPods; corresponde al dispositivo Android.
- Exigir tres transiciones con el mismo diferencial antes de declarar un patrón.
- Comparar también la transición inversa.
- Separar tráfico saliente, entrante y publicidad BLE.
- No implementar escritura hasta conocer transporte, canal, longitud, secuencia, confirmación y comportamiento ante error.

## Privacidad

Un bugreport o BTSnoop puede contener direcciones Bluetooth, nombres de dispositivos, actividad del sistema y tráfico de otros accesorios. No debe publicarse en GitHub ni enviarse a terceros. Puede mantenerse localmente en este proyecto para análisis.

## Resultado esperado

Una captura puede demostrar que una función es detectable. Eso no demuestra todavía que sea controlable. Para habilitar un control desde la app deben existir:

1. Un comando saliente estable.
2. Una respuesta o cambio de estado verificable.
3. Repetibilidad en el mismo modelo.
4. Degradación segura cuando el comando no sea compatible.
5. Ausencia de permisos privilegiados, evasión de seguridad o credenciales.
