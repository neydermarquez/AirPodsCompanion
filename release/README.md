# AirPods Companion 1.0.3

## Descarga para Android

Instala `AirPods-Companion-v1.0.3.apk`.

- Requiere Android 7.0 (API 24) o una versión posterior.
- Android puede solicitar autorización para instalar aplicaciones desde el navegador o gestor de archivos.
- El APK está firmado con la clave de producción del proyecto.
- El identificador estable de la aplicación es `com.soren.airpodscompanion`.

## Google Play

`AirPods-Companion-v1.0.3.aab` está destinado a Google Play Console. No se instala directamente en un teléfono.

## Integridad

Comprueba los archivos con las sumas SHA-256 incluidas en `SHA256SUMS.txt`.

## Novedades

- Reconciliación automática más rápida de conexiones A2DP y HFP.
- Presentación circular de batería y separación por auricular cuando existan lecturas reales.
- Lectura de códec compatible con distintas versiones de Android.
- Administrador interno con información Bluetooth y nombre local personalizable.
- Notificaciones persistentes sincronizadas con el estado actual.
- Capturas de laboratorio con finalización fiable y resultados explícitos.

## Puente nativo

La versión 1.0.3 incluye el puente JNI habilitado por defecto y bibliotecas para `arm64-v8a`, `armeabi-v7a`, `x86` y `x86_64`. Es una ruta experimental con respaldo por reflexión: su disponibilidad depende del fabricante y de Android, y no convierte una función bloqueada en compatible.

## Compatibilidad

La aplicación muestra únicamente los datos que Android o el dispositivo publican realmente. La batería individual, los modos propietarios y algunos sensores pueden variar según el modelo de AirPods, el fabricante del teléfono y la versión de Android.

## Firma

Certificado SHA-256:

`28dcb6d5a7dabdabc97a7d2ba30bcbc8cfcd5a9fc2fa8e19c6debe29cc371c75`

La clave privada y sus contraseñas no deben subirse al repositorio. Conserva copias seguras de `release-keystore.jks` y `keystore.properties`; serán necesarias para publicar actualizaciones compatibles.
