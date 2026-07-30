# Guía de release

La versión de referencia actual es `v1.0.1`. Desde esta versión, el puente nativo se compila por defecto.

## Firma

1. Crear un keystore de carga en un equipo seguro.
2. Copiar `keystore.properties.example` como `keystore.properties`.
3. Completar la ruta y contraseñas sin versionar ninguno de esos archivos.
4. Conservar una copia cifrada del keystore y sus credenciales.
5. Activar Play App Signing al publicar.

El proyecto nunca usa la clave debug como sustituto de una firma release.

## Compilación

Requisitos adicionales:

- Android NDK `28.2.13676358`.
- CMake `3.22.1`.

```powershell
.\gradlew.bat clean :app:testDebugUnitTest :app:lintRelease :app:assembleRelease :app:bundleRelease --no-configuration-cache --no-daemon
```

- APK: `app/build/outputs/apk/release/app-release.apk`.
- AAB: `app/build/outputs/bundle/release/app-release.aab`.

Para una compilación puntual sin puente nativo:

```powershell
.\gradlew.bat -PenableHiddenApiNativeBridge=false assembleRelease
```

## Versionado

- Incrementar `versionCode` en cada entrega.
- Cambiar `versionName` solo cuando corresponda a una versión pública.
- Mantener un registro de cambios verificable.

## Lista previa a publicación

- Completar identidad legal, correo y jurisdicción.
- Publicar la política de privacidad en una URL estable.
- Revisar términos con asesoría jurídica.
- Confirmar que la ficha no sugiera afiliación con Apple.
- Verificar permisos y formulario de seguridad de datos.
- Ejecutar pruebas unitarias, lint y build release.
- Validar la firma del APK y del AAB.
- Confirmar que el APK contiene `libbluetooth_hidden_bridge.so` para las ABI previstas.
- Instalar el APK release y comprobar `versionCode`, `versionName`, arranque, excepciones fatales y ANR.
- Generar `SHA256SUMS.txt` después de producir los binarios definitivos.
- Crear el tag sobre el mismo commit usado para compilar los binarios.
- Probar desde el canal interno antes de producción.

## Archivos de GitHub Release

- `AirPods-Companion-v<versión>.apk`: instalación directa.
- `AirPods-Companion-v<versión>.aab`: Google Play.
- `SHA256SUMS.txt`: verificación de integridad.

Los archivos “Source code” los genera GitHub automáticamente. La Release debe apuntar al commit exacto de compilación y no marcarse como pre-release cuando sea la versión pública estable.
