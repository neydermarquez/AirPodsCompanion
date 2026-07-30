# Declaración de seguridad de datos

Esta guía refleja el código actual. Debe volver a comprobarse en cada release.

## Recopilación y compartición

- Datos recopilados fuera del dispositivo: **No**.
- Datos compartidos con terceros: **No**.
- Publicidad: **No**.
- SDK de analítica o fallos remoto: **No**.

Los datos Bluetooth, preferencias, historial, métricas e informes se procesan localmente. Una exportación iniciada por el usuario no constituye transmisión automática por el desarrollador.

## Seguridad

- Datos cifrados en tránsito: no aplica porque la aplicación no transmite datos.
- Solicitud de eliminación: disponible dentro de la aplicación.
- Cuenta obligatoria: no.

## Tipos de datos locales

- Interacciones con la aplicación: contadores opcionales sin identificador.
- Diagnóstico: informes opcionales de fallos locales.
- Información de dispositivo: modelo del teléfono y versión Android dentro de informes locales autorizados.
- Dispositivos cercanos: nombre y estado Bluetooth usados para prestar la función principal.

## Cambios que obligan a revisar el formulario

- Añadir permiso `INTERNET`.
- Incorporar Crashlytics, Sentry, Firebase Analytics u otro SDK remoto.
- Añadir cuentas, nube, publicidad o suscripciones.
- Transmitir exportaciones o diagnósticos.
