# Guía de release

## Firma

1. Crear un keystore de carga en un equipo seguro.
2. Copiar `keystore.properties.example` como `keystore.properties`.
3. Completar la ruta y contraseñas sin versionar ninguno de esos archivos.
4. Conservar una copia cifrada del keystore y sus credenciales.
5. Activar Play App Signing al publicar.

El proyecto nunca usa la clave debug como sustituto de una firma release.

## Compilación

```powershell
.\gradlew.bat clean test lintRelease bundleRelease
```

El AAB resultante se genera en `app/build/outputs/bundle/release/`.

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
- Validar la firma del AAB.
- Probar desde el canal interno antes de producción.
