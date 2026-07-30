# Migración del identificador provisional

El identificador definitivo es `com.soren.airpodscompanion`. Android no permite actualizar directamente una aplicación instalada con `com.example.airpodscompanion` porque son identidades distintas.

## Estado actual

La versión pública `v1.0.1` utiliza `com.soren.airpodscompanion` y `versionCode` 2. Este identificador ya debe considerarse estable. Las siguientes versiones deben conservarlo y utilizar la misma clave de firma para instalarse como actualización.

## Si la versión provisional nunca se distribuyó

No se requiere migración. Desinstalar manualmente la compilación provisional de los dispositivos de desarrollo cuando ya no se necesite.

## Si llegó a usuarios

La alternativa segura es:

1. Publicar una última actualización de la aplicación provisional firmada con su clave original.
2. Permitir exportar sus datos a un archivo elegido por el usuario.
3. Instalar la aplicación definitiva.
4. Importar el archivo únicamente tras confirmación explícita.
5. Informar que el emparejamiento Bluetooth pertenece a Android y no se transfiere como dato privado de la app.

No se debe crear un proveedor exportado, almacenamiento mundialmente legible ni una puerta de migración automática entre paquetes.
