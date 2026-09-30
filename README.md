# RecordArte (Android)

Aplicación personal para registrar actividades sin servidor ni cuenta.

Los datos de tareas, notas, historial y preferencias se guardan únicamente en el
almacenamiento privado de la aplicación en el teléfono; nunca forman parte del código
subido a GitHub.

## Funciones actuales (versión 0.40.0)

- Crear actividades.
- Marcar actividades como realizadas.
- Eliminar actividades.
- Permitir crear actividades con fecha y hora o sin vencimiento.
- Mostrar explícitamente el modo **Sin vencimiento** en el formulario y debajo del
  nombre de cada tarea que no tenga fecha límite; también puede seleccionarse al editar.
- Conservar los datos localmente aunque se cierre la aplicación.
- Mostrar tareas con fecha en un widget de la pantalla de inicio.
- Redimensionar el widget entre 2×2 y 4×4; en tamaños compactos se simplifica
  el contenido para conservar la legibilidad.
- Configurar el widget para mostrar solo el día actual o cinco días contando el actual.
- Mostrar todas las fechas con el formato fijo `DD/MM/YYYY` y hora de 24 horas.
- Interpretar fechas, horas y rangos diarios con la zona `America/Argentina/Buenos_Aires`.
- Bloquear las tareas cumplidas, ocultar su alerta de vencimiento y eliminarlas
  automáticamente al finalizar el día en el horario de Buenos Aires.
- Guardar un historial local separado de tareas finalizadas y tareas vencidas.
- Mantener cada ocurrencia en un único estado del historial: si fue realizada queda
  solamente en Finalizadas; si terminó el día sin cumplirse queda solamente en Vencidas.
  Al actualizar, también se eliminan automáticamente los duplicados históricos existentes.
- Eliminar, con confirmación, los registros del mes actual o todo el historial local.
- Resolver tareas de días anteriores desde el historial: agregar o editar su nota,
  o moverlas a Finalizadas como **realizadas fuera de término**, conservando tanto
  la fecha original de vencimiento como la fecha de la corrección.
- Consultar desde Configuración todas las tareas realizadas durante el mes actual.
- Mantener en el widget las tareas vencidas con una alerta `⚠`.
- Elegir entre **Widget del día** y **Widget semanal** (hoy más cuatro días).
- Elegir de forma independiente el fondo de la aplicación entre cinco tonos claros
  y cinco tonos oscuros.
- Alternar entre tema claro y oscuro para la aplicación y el widget.
- Mostrar el logotipo original de RecordArte, con su pincelada, y bordes transparentes
  limpios tanto en ambos temas como en el widget.
- Permitir corregir durante cinco minutos una tarea marcada como cumplida por error;
  después de ese plazo queda bloqueada hasta finalizar el día.
- Guardar una nota local de varias líneas en cada tarea, con opciones para leerla,
  editarla o borrarla desde el botón **Nota**.
- Detectar direcciones `http`, `https` y `www` dentro de las notas y abrirlas en el
  navegador al tocarlas desde la tarea, el historial o el calendario.
- Desplazar verticalmente todas las tareas disponibles desde el widget, sin el
  límite anterior de tres elementos.
- Consultar desde el historial la nota guardada con cada tarea finalizada o vencida.
- Buscar en el historial por nombre de tarea o por el contenido de sus notas.
- Agrupar el historial en secciones plegables de tareas diarias, semanales, mensuales
  y únicas; los registros antiguos sin frecuencia identificable se mantienen en un
  grupo separado para no clasificarlos incorrectamente.
- Mantener cada nota asociada únicamente a su ocurrencia: queda guardada en el
  historial correspondiente y la siguiente repetición comienza sin nota.
- Crear tareas en el orden nombre, fecha y hora opcionales, frecuencia opcional y
  botón **Agregar** al final.
- Elegir la frecuencia **Única**, **Diaria**, **Semanal** o **Mensual**; si no se
  selecciona otra opción, la tarea es única por defecto.
- Al comenzar un nuevo día, archivar las tareas únicas incumplidas en el historial
  de vencidas y retirarlas de las tareas activas.
- En tareas recurrentes incumplidas, archivar la ocurrencia anterior y generar la
  correspondiente al día vigente o la próxima fecha programada, sin duplicarla.
- Ordenar las tareas activas por vencimiento, desde la fecha más próxima hasta la
  más lejana; las tareas sin fecha quedan después y las cumplidas al final.
- Filtrar rápidamente el listado entre **Todas**, **Hoy** y **Otras**; este último
  grupo reúne las actividades futuras y las que no tienen fecha.
- Abrir desde la cabecera principal un calendario mensual, navegar entre meses y
  tocar cualquier día para consultar sus tareas activas, finalizadas o vencidas.
- Mostrar en cada fecha del calendario un indicador con la cantidad de tareas y
  conservar el formato `DD/MM/YYYY` y el horario de Buenos Aires.
- Mantener todas las celdas del calendario alineadas y con bordes completos incluso
  cuando muestran contadores de varias líneas, tanto en tema claro como oscuro.
- Presentar una interfaz visual más compacta y consistente, con formulario agrupado,
  tarjetas redondeadas, botones jerarquizados y colores según el estado de cada tarea.
- Acceder mediante un engranaje a una pantalla separada para las preferencias
  generales de apariencia y widget.
- Identidad visual **RecordArte** e icono propio con reloj, pincel y paleta.
- Logotipo tipográfico RecordArte en la cabecera principal.
- Logotipo RecordArte como título gráfico del widget.
- Opción temporal para deshacer la eliminación de una tarea.
- Pantalla de información con desarrollador, almacenamiento local, horario y versión.
- Recibir una notificación al llegar la fecha y hora de una actividad.
- Completar, posponer 15 minutos o pasar para mañana una actividad desde la notificación.
- Repetir actividades todos los días, todas las semanas o todos los meses.
- Generar la próxima ocurrencia de una actividad recurrente recién al retirar de
  la pantalla la ocurrencia cumplida del período anterior.
- Editar el nombre, la fecha, la hora y la repetición de cualquier actividad.
- Reprogramar rápidamente una actividad para hoy o mañana.

## Agregar el widget

1. Mantener presionado un espacio vacío de la pantalla de inicio.
2. Elegir **Widgets**.
3. Buscar **RecordArte**.
4. Mantener presionado **Widget de actividades** y arrastrarlo a la pantalla.

## Abrir y generar el APK

1. Instalar Android Studio con el Android SDK 34 (o posterior) y JDK 17.
2. En Android Studio elegir **Open** y seleccionar la carpeta `mis-actividades-android`.
3. Esperar a que finalice la sincronización de Gradle.
4. Elegir **Build > Build APK(s)**.
5. El APK de prueba queda en `app/build/outputs/apk/debug/app-debug.apk`.

Para instalarlo por USB, habilitar la depuración USB y ejecutar:

```powershell
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

También se puede copiar el APK al teléfono, abrirlo y autorizar la instalación desde esa fuente.

Al abrir la versión 0.40.0 por primera vez, Android solicita permiso para mostrar
notificaciones. Los recordatorios se conservan localmente y se vuelven a programar
después de reiniciar el teléfono.

## Evolución prevista

- Widgets configurables, cada uno asociado a una actividad concreta.

No se necesita servidor ni cuenta para estas funciones.

## Estructura técnica

El repositorio usa la estructura estándar de un proyecto Android con Gradle: el código
y los recursos instalables viven en `app/src/main`, el Gradle Wrapper permite compilar
la misma versión desde cualquier equipo y los archivos locales o generados quedan
excluidos mediante `.gitignore`.

La explicación de responsabilidades, persistencia, notificaciones y widget está en
[`docs/ARCHITECTURE.md`](docs/ARCHITECTURE.md).

Cada `push` o pull request sobre `main` ejecuta una compilación automática en GitHub
Actions y conserva el APK de depuración como artefacto descargable de esa ejecución.
