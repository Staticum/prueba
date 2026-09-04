# Transcripción de Audio con Identificación de Hablantes (Android)

App Android nativa (Kotlin + Jetpack Compose) para:

1. Subir una grabación de audio desde el teléfono.
2. Transcribirla automáticamente y separar el texto por hablante (diarización), etiquetados como "Hablante A", "Hablante B", etc.
3. Escuchar un extracto de audio de cada hablante y asignarle un nombre real, que se aplica a toda la transcripción.

## Cómo funciona la transcripción

La transcripción y la identificación de hablantes se hacen con el servicio en la nube
[AssemblyAI](https://www.assemblyai.com/), que soporta diarización de forma nativa
(parámetro `speaker_labels`). Necesitas tu propia API key gratuita:

1. Crea una cuenta en https://www.assemblyai.com/ (tiene plan gratuito).
2. Copia tu API key desde el dashboard.
3. Pégala en el campo "API key de AssemblyAI" al abrir la app.

La API key se mantiene solo en memoria mientras la app está abierta; no se envía a ningún
otro servicio.

## Compilar

Requiere Android Studio (Koala o superior) o el SDK de Android + JDK 17.

```
./gradlew assembleDebug
```

Si es la primera vez que abres el proyecto, Android Studio generará automáticamente el
`gradle-wrapper.jar` (no se versiona por ser binario). También puedes generarlo a mano con:

```
gradle wrapper --gradle-version 8.7
```

El APK debug queda en `app/build/outputs/apk/debug/app-debug.apk`.

## Uso

1. Abre la app, ingresa tu API key.
2. Toca "Elegir audio" y selecciona el archivo (mp3, wav, m4a, etc.).
3. Toca "Transcribir" y espera (el tiempo depende de la duración del audio).
4. Verás la transcripción dividida en segmentos, cada uno con su hablante genérico
   ("Hablante A", "Hablante B", ...).
5. Toca "▶ escuchar" en un segmento para oír ese extracto y confirmar de quién es la voz.
6. Toca "✎ renombrar" para asignarle un nombre real a ese hablante — se actualiza en
   todos los segmentos de esa misma persona y queda guardado para esa transcripción.

## Estructura del proyecto

- `data/` — cliente Retrofit de AssemblyAI, modelos y repositorio de transcripción; Room
  para persistir el nombre asignado a cada hablante.
- `player/` — reproductor de extractos de audio (ExoPlayer) acotado a los tiempos de
  inicio/fin de cada segmento de habla.
- `ui/` — pantalla en Compose y ViewModel.

## Posibles mejoras futuras

- Guardar la API key de forma cifrada (EncryptedSharedPreferences) en vez de solo en memoria.
- Exportar la transcripción final (con nombres) a texto o PDF.
- Soportar grabación de audio directamente desde el micrófono, además de archivos subidos.
