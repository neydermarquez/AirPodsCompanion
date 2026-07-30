# Guía inicial para crear aplicaciones Android

> **Estado del documento:** guía histórica de planificación. AirPods Companion ya fue implementada y su versión pública actual es `v1.0.2`. Para conocer el estado real consulta `README.md`, `docs/REQUIREMENTS_TRACEABILITY.md` y `HARDWARE_TEST_MATRIX.md`. Las secciones redactadas en futuro describen el alcance original, no trabajo pendiente confirmado.

## Tecnología elegida

Para comenzar a desarrollar la aplicación utilizaremos la siguiente combinación:

- **Kotlin:** lenguaje de programación principal.
- **Jetpack Compose:** herramienta moderna para crear interfaces gráficas.
- **Android Studio:** entorno de desarrollo oficial para Android.
- **Material 3:** componentes visuales y sistema de diseño.

La combinación principal será:

```text
Kotlin + Jetpack Compose + Android Studio
```

## Requisitos

Antes de comenzar, será necesario instalar:

1. Android Studio.
2. Android SDK.
3. Kotlin, incluido normalmente en Android Studio.
4. Un emulador Android o un teléfono físico para probar la aplicación.

## Crear el proyecto

En Android Studio:

1. Seleccionar **New Project**.
2. Elegir **Empty Activity**.
3. Seleccionar **Kotlin** como lenguaje.
4. Usar **Jetpack Compose** para la interfaz.
5. Elegir un nombre para la aplicación.
6. Crear el proyecto.

## Estructura recomendada

La aplicación se organizará de forma clara y escalable:

```text
app/
└── src/main/java/com/ejemplo/app/
    ├── MainActivity.kt
    ├── ui/
    │   ├── components/
    │   ├── screens/
    │   └── theme/
    ├── data/
    ├── domain/
    └── navigation/
```

### Descripción de las carpetas

- **ui:** pantallas, componentes y estilos visuales.
- **components:** botones, tarjetas, barras y elementos reutilizables.
- **screens:** pantallas principales de la aplicación.
- **theme:** colores, tipografías y formas.
- **data:** fuentes de datos, API, base de datos o almacenamiento local.
- **domain:** reglas y lógica principal del negocio.
- **navigation:** navegación entre pantallas.

## Ejemplo básico de pantalla

```kotlin
package com.ejemplo.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            PantallaPrincipal()
        }
    }
}

@Composable
fun PantallaPrincipal() {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("Mi aplicación Android")

        Button(onClick = { }) {
            Text("Comenzar")
        }
    }
}
```

## Principios de desarrollo

- Crear componentes reutilizables.
- Mantener separada la interfaz de la lógica de negocio.
- Diseñar primero las pantallas y el flujo de navegación.
- Usar nombres claros para archivos, funciones y variables.
- Probar la aplicación en diferentes tamaños de pantalla.
- Adaptar la interfaz para teléfonos y tabletas.
- Mantener accesibles los botones, textos y controles.

## Diseño similar al de iPhone

La aplicación puede tener una apariencia inspirada en iOS mediante:

- Diseño limpio y minimalista.
- Bordes redondeados.
- Espaciado amplio.
- Tipografía sencilla.
- Animaciones suaves.
- Barras de navegación inferiores.
- Colores neutros y controles personalizados.

La interfaz debe seguir siendo cómoda y coherente para usuarios de Android.

## Próximos pasos

Cuando se defina la aplicación, se prepararán:

1. Objetivo principal.
2. Lista de funciones.
3. Pantallas necesarias.
4. Flujo de navegación.
5. Diseño visual.
6. Modelo de datos.
7. Implementación en Kotlin y Jetpack Compose.
8. Pruebas y corrección de errores.

## Objetivo del proyecto

Este documento servirá como base técnica para comenzar a desarrollar la aplicación Android que se describa posteriormente, utilizando una arquitectura moderna, escalable y con una interfaz inspirada en la experiencia de iPhone.

---

# Proyecto AirPods Companion

## Objetivo

Crear una aplicación Android capaz de ofrecer la experiencia más completa posible con los AirPods, sin root y sin modificar el teléfono. La implementación prioriza APIs oficiales de Android y Bluetooth; desde `v1.0.1` incorpora además un puente nativo experimental con degradación segura cuando el sistema bloquea una consulta.

La aplicación no intentará hacer que Android se identifique falsamente como un iPhone. Funcionará como una aplicación complementaria que interpreta las capacidades disponibles de cada modelo.

## Modelos que debe contemplar

- AirPods (1.ª generación).
- AirPods (2.ª generación).
- AirPods (3.ª generación).
- AirPods 4.
- AirPods 4 con cancelación activa de ruido.
- AirPods Pro (1.ª generación).
- AirPods Pro 2 con estuche Lightning.
- AirPods Pro 2 con estuche USB-C.
- AirPods Pro 3.
- AirPods Max con Lightning.
- AirPods Max con USB-C.
- AirPods Max 2.

## Inventario de funciones

La aplicación debe contemplar, investigar y documentar las siguientes funciones:

### Conexión y estado

- Emparejamiento y reconexión automática.
- Detección del modelo de AirPods.
- Detección de conexión y desconexión.
- Estado de cada auricular.
- Estado del estuche.
- Estado de carga.
- Batería individual de los auriculares y del estuche cuando esté disponible.
- Registro del último estado conectado.
- Diagnóstico de conexión, latencia y micrófono.
- Registro de errores.
- Modo de bajo consumo.

### Audio y llamadas

- Reproducción y pausa.
- Siguiente pista y pista anterior.
- Control de volumen.
- Audio multimedia.
- Llamadas telefónicas.
- Uso del micrófono.
- Silenciar y activar el micrófono durante llamadas.
- Control multimedia desde la pantalla de bloqueo.
- Compatibilidad con los códecs Bluetooth disponibles en Android.

### Sensores y controles físicos

- Doble toque de AirPods 1 y 2.
- Sensor de presión de AirPods 3, AirPods 4 y AirPods Pro.
- Digital Crown de AirPods Max.
- Botón de modo de escucha de AirPods Max.
- Personalización de acciones.
- Detección de colocación en el oído.
- Pausa automática al retirar un auricular.
- Reanudación automática al colocarlo nuevamente.

### Modos de escucha

- Cancelación activa de ruido.
- Modo Transparencia.
- Audio adaptativo.
- Conciencia de conversación.
- Volumen personalizado.
- Ecualización adaptativa.
- Reducción de sonidos fuertes, cuando el modelo y el sistema lo permitan.

### Audio espacial

- Audio espacial.
- Seguimiento dinámico de la cabeza.
- Audio espacial personalizado.
- Configuración y pruebas de compatibilidad por modelo.

### Gestos y accesibilidad

- Gestos de cabeza en los modelos compatibles.
- Aceptar o rechazar llamadas mediante gestos, cuando sea posible.
- Interacción con notificaciones mediante gestos, cuando sea posible.
- Anuncios de llamadas y notificaciones mediante alternativas de Android.
- Perfiles para música, llamadas, juegos y oficina.
- Opciones de accesibilidad y controles de tamaño y contraste.

### Interfaz de la aplicación

- Ventana emergente de conexión inspirada en el estilo del iPhone.
- Widgets de batería y conexión.
- Notificaciones de conexión y desconexión.
- Pantalla de información por modelo.
- Indicadores de compatibilidad por función.
- Configuración de nombre y preferencias.
- Panel de diagnóstico.
- Explicación clara de permisos y privacidad.

## Funciones relacionadas con Apple

Algunas funciones no dependen solamente de Bluetooth y pueden requerir iOS, iPadOS, macOS, iCloud, Apple Intelligence o firmware específico. La aplicación no debe prometer compatibilidad total con ellas.

Entre las funciones potencialmente limitadas están:

- Siri.
- Integración con iCloud.
- Cambio automático entre dispositivos Apple.
- Red Buscar completo.
- Actualizaciones de firmware.
- Configuraciones internas protegidas.
- Funciones avanzadas de salud auditiva.
- Prueba de audición.
- Función de audífono.
- Protección auditiva avanzada.
- Traducción en vivo y funciones de inteligencia de Apple.

Estas funciones deberán aparecer en la aplicación, pero con su estado real de compatibilidad.

## Estados de compatibilidad

Cada función debe clasificarse utilizando uno de estos estados:

| Estado | Significado |
|---|---|
| Compatible | La función funciona en Android con el modelo probado. |
| Compatible con limitaciones | Funciona parcialmente o requiere permisos, versión de Android o hardware específico. |
| No disponible en Android | La función depende de servicios, protocolos o dispositivos de Apple. |
| Pendiente de investigación | Todavía no se ha verificado de forma segura y reproducible. |

## Arquitectura técnica prevista

```text
Kotlin
├── Jetpack Compose: interfaz y pantallas
├── Bluetooth Classic: audio, llamadas y controles estándar
├── Bluetooth Low Energy: estado, sensores y funciones compatibles
├── MediaSession: reproducción y controles multimedia
├── Foreground Service: supervisión de conexión
├── Room: preferencias, modelos y registros locales
├── DataStore: configuración de la aplicación
└── Motor de compatibilidad: capacidades específicas por modelo
```

## Reglas de seguridad y legalidad

- No utilizar root.
- No modificar el sistema operativo.
- No instalar firmware alterado.
- No falsificar credenciales de Apple.
- No evadir mecanismos de seguridad.
- No acceder a cuentas iCloud sin autorización.
- No recopilar datos innecesarios.
- Solicitar únicamente los permisos necesarios.
- Informar al usuario cuando una función no sea oficialmente compatible.
- Probar cada función con modelos reales y documentar sus resultados.

## Plan de desarrollo

1. Crear el proyecto Android con Kotlin y Jetpack Compose.
2. Implementar detección y conexión Bluetooth.
3. Mostrar batería y estado de conexión.
4. Implementar controles de audio y llamadas.
5. Añadir sensores, gestos y detección de colocación.
6. Investigar por modelo los modos ANC, Transparencia y audio espacial.
7. Crear la pantalla de compatibilidad.
8. Añadir widgets, notificaciones y ventana emergente.
9. Crear diagnósticos y registros de errores.
10. Probar cada modelo y cada función en varios teléfonos Android.
11. Marcar las funciones incompatibles sin ocultarlas.
12. Revisar privacidad, estabilidad y consumo de batería.

## Criterio de calidad

La aplicación se considerará correctamente implementada cuando cada función del inventario tenga un resultado documentado:

```text
Compatible
Compatible con limitaciones
No disponible en Android
Pendiente de investigación
```

No se debe afirmar que una función está disponible sin probarla en el modelo correspondiente y en una versión concreta de Android.

## Fuentes de investigación

- [Comparación oficial de modelos AirPods](https://www.apple.com/airpods/compare/)
- [Manual oficial de AirPods](https://support.apple.com/guide/airpods/welcome/web)
- [Controles y gestos de AirPods](https://support.apple.com/guide/airpods/devb2c431317/web)
- [Uso de AirPods con dispositivos no Apple](https://support.apple.com/guide/airpods/dev499c9718b/web)
- [Especificaciones de AirPods Pro 2](https://support.apple.com/en-la/111834)
