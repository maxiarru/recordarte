# Arquitectura de RecordArte

RecordArte es una aplicación Android nativa, de un solo módulo y pensada para uso
personal sin servidor. La estructura sigue las convenciones de Gradle para Android y
mantiene separadas las responsabilidades aunque el tamaño actual no justifica agregar
frameworks o módulos adicionales.

## Estructura del repositorio

```text
mis-actividades-android/
├── .github/workflows/       Verificación automática de la compilación
├── app/                     Módulo Android instalable
│   └── src/main/
│       ├── java/com/cerus/misactividades/
│       ├── res/             Textos, temas, imágenes, layouts y configuración del widget
│       └── AndroidManifest.xml
├── docs/                    Documentación técnica
├── gradle/wrapper/          Gradle Wrapper versionado
├── build.gradle.kts         Plugins compartidos
└── settings.gradle.kts      Definición del proyecto y sus módulos
```

`local.properties`, `.idea`, `.gradle` y todos los directorios `build` son locales o
generados. Se excluyen de Git. El Gradle Wrapper sí se versiona para que el proyecto se
pueda compilar sin depender de una instalación global de Gradle.

## Responsabilidades internas

- **Interfaz:** `MainActivity`, `SettingsActivity`, `HistoryActivity` y
  `CalendarActivity` construyen las pantallas y responden a las acciones del usuario.
- **Dominio:** `Task`, `TaskRecurrence` y `TaskActions` representan las actividades y
  sus reglas de repetición y reprogramación.
- **Persistencia local:** `TaskRepository` conserva las tareas y
  `TaskHistoryRepository` conserva los estados finalizados o vencidos mediante
  `SharedPreferences` y JSON.
- **Tiempo:** `AppTimeZone` y `TaskDateFormatter` centralizan el horario de Buenos Aires
  y el formato `DD/MM/YYYY`.
- **Recordatorios:** `ReminderScheduler`, los receivers y
  `ReminderNotifications` programan y muestran notificaciones locales.
- **Widget:** `TasksWidgetProvider`, `TasksWidgetService` y `WidgetPreferences`
  alimentan el widget redimensionable de la pantalla de inicio.
- **Apariencia:** `AppThemePreferences`, `AppBackgroundPreferences` y `UiStyle`
  concentran tema, colores y componentes visuales reutilizables.

## Flujo de datos

```text
Pantallas / notificaciones / widget
                │
                ▼
       TaskRepository y reglas
                │
                ├── tareas activas
                ├── historial finalizado o vencido
                └── programación de recordatorios
                │
                ▼
      almacenamiento local del teléfono
```

No se suben tareas, notas ni preferencias a GitHub. Esos datos existen solamente en
el almacenamiento privado de la aplicación en cada teléfono.

## Decisiones de compatibilidad

- El `applicationId` continúa siendo `com.cerus.misactividades`. Cambiarlo haría que
  Android instalara otra aplicación y no pudiera reutilizar los datos locales actuales.
- La versión mínima es Android 8 (`minSdk 26`) y el código se compila con Java 17.
- El proyecto usa Java y las APIs nativas de Android, sin dependencias de terceros ni
  servicios en línea.

Si el proyecto crece, el siguiente paso razonable es separar físicamente los paquetes
`ui`, `data`, `domain`, `notifications` y `widget`, y agregar pruebas unitarias en
`app/src/test`. No es necesario introducir esa complejidad mientras la aplicación siga
siendo pequeña y local.
