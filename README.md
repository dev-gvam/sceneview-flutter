# SceneView Flutter
This project can be considered as a rewrite of [arcore_flutter_plugin](https://github.com/giandifra/arcore_flutter_plugin) which was based on archived Sceneform.
SceneView is a [Sceneform Maintained](https://github.com/SceneView/sceneview-android) replacement in Kotlin

## Build requirements

Ejecutar en un dispositivo compatible con ArCore Geospatial

Ejecutar al clonar
> flutter pub get

Para ejecutarla en un dispositivo si es desde vscode ejecutar el play -> Device

o bien ejecutar

> cd example

> flutter run

## Importante

No funciona en hot-reload de flutter, se debe ejecutar flutter run, cerrar la app y volverla a abrir

Se debe añadir un API_KEY de ArCore en el AndroidManifest.xml

Modificar las coordenadas del parametro positions de _controller?.loadPositions para visualizar los modelos.
 - Se pueden añadir más positions si se necesita
 - Se puede cambiar el color de las positions segun el parametro type

### TODOs
- [] Revisar y refactorizar el codigo
- [] Revisar la forma en la que se dispone el controller
- [] Revisar si se pueden controlar los eventos del pause y resume en flutter
- [] Revisar si se puede hacer livereload