# Guía de release

La versión de referencia actual es `v1.0.2`. El puente nativo se compila por defecto desde `v1.0.1`.

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
- Actualizar el README principal, el README de `release/`, la matriz de hardware y la trazabilidad cuando cambie el comportamiento técnico.
- No reutilizar un tag publicado para binarios distintos.

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

## Publicación en el repositorio

1. Confirmar que el árbol de trabajo esté limpio y que `main` contenga el commit que produjo los binarios.
2. Enviar el commit:

   ```powershell
   git push origin main
   ```

3. En GitHub, abrir **Releases** y seleccionar **Draft a new release**.
4. Crear `v<versión>` como tag nuevo con destino `main`. Si el tag ya existe, comprobar su commit antes de continuar.
5. Usar un título coherente, por ejemplo `AirPods Companion v1.0.2 (Edición única)`.
6. Documentar cambios, funciones principales, instalación, requisitos y limitaciones reales.
7. Adjuntar APK, AAB y `SHA256SUMS.txt`.
8. Dejar **Pre-release** desmarcado para una versión estable.
9. Marcar la publicación como **Latest** cuando sea la descarga recomendada.
10. Publicar y verificar en la página resultante:
    - versión y título;
    - commit del tag;
    - etiqueta **Latest**;
    - ausencia de **Pre-release**;
    - nombres, tamaños y hashes de los tres adjuntos;
    - enlace `/releases/latest` desde el README.

## Corrección de un tag equivocado

Si una Release recién creada apunta a un commit incorrecto y todavía no debe considerarse inmutable:

```powershell
git tag -f v<versión> <commit-correcto>
git push origin v<versión> --force
```

Después se debe volver a comprobar el commit mostrado por GitHub. No mover tags de versiones ya distribuidas ampliamente; en ese caso, publicar una versión nueva.

## Cierre posterior

- Descargar el APK desde GitHub y comparar su SHA-256 con `SHA256SUMS.txt`.
- Confirmar que el APK descargado conserva la firma esperada.
- Conservar la Release anterior como historial.
- No cambiar el identificador de aplicación ni la clave de firma entre actualizaciones.
- Registrar cualquier cambio documental posterior en `main` sin mover el tag, siempre que los binarios no hayan cambiado.
