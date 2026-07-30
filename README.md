<p align="center">
  <img src="app/src/main/res/drawable-nodpi/launcher_art.png" width="112" alt="Ícono de AirPods Companion">
</p>

<h1 align="center">AirPods Companion</h1>

<p align="center">
  Consulta desde Android la conexión, batería, audio y compatibilidad que tus AirPods publiquen realmente.
</p>

<p align="center">
  <a href="https://github.com/neydermarquez/AirPodsCompanion/releases/latest">
    <img src="https://img.shields.io/github/v/release/neydermarquez/AirPodsCompanion?label=Descargar&color=1769e0" alt="Última versión">
  </a>
  <img src="https://img.shields.io/badge/Android-7.0%2B-3f7f6b" alt="Android 7.0 o posterior">
  <img src="https://img.shields.io/badge/Kotlin-Jetpack%20Compose-526b88" alt="Kotlin y Jetpack Compose">
</p>

## Qué es

AirPods Companion es una aplicación independiente para Android que reconoce AirPods vinculados o conectados y presenta únicamente la información que Android y el dispositivo hacen disponible. No requiere root, una cuenta de Apple ni modificaciones del sistema.

La interfaz diferencia los datos actuales, las lecturas antiguas y las funciones que Android no permite controlar. No utiliza valores simulados para aparentar compatibilidad.

La versión actual utiliza una interfaz clara única, con superficies suaves, navegación inferior persistente y estados específicos para permisos, Bluetooth apagado, búsqueda, conexión, reconexión, pérdida de conexión y compatibilidad limitada.

## Capturas

<p align="center">
  <img src="docs/screenshots/permissions.png" width="23%" alt="Solicitud de permiso para dispositivos cercanos">
  <img src="docs/screenshots/devices.png" width="23%" alt="Búsqueda y conexión de dispositivos">
  <img src="docs/screenshots/activity.png" width="23%" alt="Historial local de actividad">
  <img src="docs/screenshots/settings.png" width="23%" alt="Preferencias, notificaciones y supervisión">
</p>

## Funciones principales

- Reconocimiento automático de AirPods vinculados y conectados.
- Búsqueda de dispositivos cercanos y emparejamiento mediante Android.
- Estado real de Bluetooth, conexión, desconexión y reconexión.
- Batería general o individual cuando el modelo la publique.
- Vigencia y fuente de cada lectura de batería.
- Controles multimedia compatibles: reproducción, pausa, pistas y volumen.
- Diagnóstico de rutas multimedia, micrófono y llamadas.
- Audio espacial y seguimiento de cabeza cuando Android los exponga.
- Historial local de conexiones, errores y diagnósticos.
- Notificaciones configurables y supervisión opcional en segundo plano.
- Widget de conexión y batería.
- Información de compatibilidad por modelo y capacidad.
- Perfiles para música, llamadas, juegos y oficina.
- Preferencias independientes de volumen, avisos y diagnósticos por perfil.
- Exportación y eliminación de los datos locales.

## Más de lo que muestra la pantalla principal

La aplicación incorpora varias funciones de soporte que trabajan detrás de la interfaz:

- Inventario de **41 capacidades** con un estado explícito para cada modelo reconocido.
- Repositorio Bluetooth único compartido por la actividad y el servicio de supervisión.
- Servicio en primer plano con estado visible, registro de recuperaciones y reinicio opcional después de encender el teléfono.
- Asociación opcional mediante Companion Device Manager en versiones compatibles de Android.
- Recomendaciones de segundo plano específicas para Samsung, Xiaomi, Motorola, OnePlus, Oppo, Realme, Huawei y Honor.
- Notificaciones separadas para conexión, desconexión y batería baja, con acción de reconexión.
- Widget automático, compacto o detallado, con estados de permisos, Bluetooth, reconexión y batería por componente.
- Retención configurable para el historial y las muestras técnicas.
- Migración automática de preferencias antiguas a DataStore.
- Historial y evidencia técnica almacenados localmente con Room.

## Laboratorio de compatibilidad

AirPods Companion incluye un laboratorio local para registrar y comparar señales de:

- Cancelación de ruido.
- Transparencia.
- Audio adaptativo.
- Auricular izquierdo y derecho.
- Estuche abierto.
- Carga.
- Gestos o controles físicos.

Una capacidad pendiente solo puede pasar a **Detectable** cuando existen diferencias estables y reproducibles en tres o más muestras. Las sesiones pueden compararse, eliminarse o exportarse de forma anónima con autorización del usuario.

## Estados transparentes

Cada capacidad se presenta con un estado explícito:

- **Controlable:** la aplicación puede ejecutar la acción.
- **Detectable:** Android publica el estado, pero no necesariamente permite cambiarlo.
- **Control físico:** depende de los controles del auricular.
- **Gestionado por Android:** el sistema o el reproductor son responsables.
- **No disponible:** Android no ofrece una API pública compatible.
- **Pendiente de evidencia:** requiere validación reproducible con hardware real.

Funciones propietarias como Buscar, iCloud, Siri, actualización de firmware y personalizaciones exclusivas del ecosistema Apple no se presentan como disponibles en Android.

El código fuente actual compila y carga por defecto un puente nativo experimental para ampliar el diagnóstico Bluetooth cuando el dispositivo lo permita. El puente intenta primero la ruta JNI y conserva una ruta de respaldo por reflexión; cualquier fallo se trata de forma segura y no convierte una API no disponible en una capacidad garantizada.

Puede desactivarse para una compilación concreta con:

```powershell
.\gradlew.bat -PenableHiddenApiNativeBridge=false assembleDebug
```

> **Nota sobre `v1.0.0`:** los APK y AAB adjuntos a esa Release se generaron antes de activar el puente por defecto. El cambio está presente en el código de `main` desde el commit `4f8128d` y requiere generar una nueva compilación para llegar a un binario descargable.

## Descargar e instalar

1. Abre la [última versión publicada](https://github.com/neydermarquez/AirPodsCompanion/releases/latest).
2. Descarga `AirPods-Companion-v1.0.0.apk`.
3. Abre el archivo en tu dispositivo Android.
4. Si Android lo solicita, autoriza temporalmente la instalación desde el navegador o gestor de archivos.

Requiere Android 7.0 (API 24) o posterior. El archivo `.aab` de la publicación está destinado a Google Play y no se instala directamente.

### Verificar la descarga

La publicación incluye `SHA256SUMS.txt`. En Windows puedes comprobar el APK con:

```powershell
Get-FileHash .\AirPods-Companion-v1.0.0.apk -Algorithm SHA256
```

## Privacidad

- Procesamiento local.
- Sin publicidad.
- Sin cuenta de Apple.
- Sin recopilación ni almacenamiento de ubicación.
- Sin transmisión automática de diagnósticos.
- Historial y evidencia técnica eliminables desde la aplicación.

En Android 11 o versiones anteriores, el sistema puede asociar el escaneo Bluetooth con el permiso de ubicación debido a su modelo histórico de permisos. AirPods Companion no utiliza ese permiso para obtener ni guardar la ubicación del usuario. En Android 12 o posterior se emplean los permisos de dispositivos cercanos.

Consulta la [política de privacidad](docs/legal/PRIVACY_POLICY.md) y la [declaración de independencia](docs/legal/APPLE_INDEPENDENCE_NOTICE.md).

## Compilar el proyecto

Requisitos:

- Android Studio con JDK 11 o posterior.
- Android SDK 36.
- Android NDK y CMake 3.22.1 para la configuración predeterminada con puente nativo.

En Windows:

```powershell
.\gradlew.bat assembleDebug
```

Para compilar sin código nativo:

```powershell
.\gradlew.bat -PenableHiddenApiNativeBridge=false assembleDebug
```

Para ejecutar pruebas y análisis:

```powershell
.\gradlew.bat testDebugUnitTest lintRelease
```

La firma de producción no forma parte del repositorio. Cada distribuidor debe configurar su propia clave siguiendo `keystore.properties.example`.

## Tecnología

- Kotlin
- Jetpack Compose
- Material 3
- Bluetooth Classic, A2DP y HFP
- DataStore
- Room
- ViewModel y restauración de estado
- Companion Device Manager
- JNI, Android NDK y CMake para el puente Bluetooth experimental
- Servicios y notificaciones de Android
- Widgets de aplicación
- R8 y reducción de recursos en release

## Estado del proyecto

La versión pública actual es `v1.0.0`. La aplicación está compilada, firmada y disponible para instalación. La arquitectura funcional y la presentación de estados están implementadas; la validación detallada de batería, carga, modos propietarios y sensores depende de disponer de cada generación de AirPods y teléfonos Android físicos.

Consulta la [trazabilidad funcional](docs/REQUIREMENTS_TRACEABILITY.md) y la [matriz de pruebas de hardware](HARDWARE_TEST_MATRIX.md) para conocer el estado exacto de cada área.

## Independencia

AirPods Companion es una aplicación independiente y no está afiliada, patrocinada ni aprobada por Apple Inc. AirPods y Apple son marcas de Apple Inc. La disponibilidad de funciones depende de las capacidades que Android y el dispositivo conectado publiquen.
