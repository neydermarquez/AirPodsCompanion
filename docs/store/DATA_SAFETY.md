# Declaración de seguridad de datos

Esta guía refleja `v1.0.2`. Debe volver a comprobarse en cada release.

## Recopilación y compartición

- Datos recopilados fuera del dispositivo: **No**.
- Datos compartidos con terceros: **No**.
- Publicidad: **No**.
- SDK de analítica o fallos remoto: **No**.

Los datos Bluetooth, preferencias, historial, métricas e informes se procesan localmente. Una exportación iniciada por el usuario no constituye transmisión automática por el desarrollador.

El acceso voluntario a Buscar abre `iCloud.com/find` en una pestaña segura del navegador. AirPods Companion no recibe las credenciales, cookies, ubicaciones ni resultados mostrados por Apple; esta navegación de terceros debe describirse en la política de privacidad pública.

El puente nativo incluido desde `v1.0.1` realiza intentos locales de diagnóstico y no añade permiso `INTERNET`, telemetría ni transmisión automática.

## Seguridad

- Datos cifrados en tránsito: AirPods Companion no transmite datos propios; el acceso opcional a Apple utiliza una dirección HTTPS gestionada por el navegador.
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
- Sustituir el acceso aislado del navegador por una integración que reciba datos de una cuenta o de iCloud.
