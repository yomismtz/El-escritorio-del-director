# El Escritorio del Director

Aplicación Android para Dirección dentro del ecosistema online formado por **ProfeCuaderno**, **El Cuaderno del Estudiante** y **El Escritorio del Director**.

## Descarga Android

Página pública de Dirección:

https://profecuaderno-api-production.up.railway.app/director

Descarga directa del APK instalable más reciente:

https://github.com/yomismtz/El-escritorio-del-director/releases/download/android-latest/El-Escritorio-del-Director.apk

El APK publicado por este flujo es una compilación de prueba para instalación directa. Android puede solicitar autorización para instalar aplicaciones desde el navegador o gestor de archivos.

## Estado actual

La aplicación ya trabaja con el backend central real. El rol Director puede:

- Crear y consultar su institución.
- Vincular docentes registrados mediante su correo exacto.
- Consultar las clases pertenecientes a la institución.
- Enviar comunicados institucionales privados a los docentes.
- Crear, consultar y eliminar bloques de horario.
- Seleccionar docente, clase, día, hora y aula con validación de conflictos en el servidor.

## Límites de privacidad

Dirección coordina la estructura institucional, pero el backend no le abre las funciones reservadas al docente para listas de alumnos, captura de asistencia, calificaciones ni detalle de coevaluaciones de equipo. Esta separación forma parte del modelo de permisos del ecosistema.

## Relación con las otras apps

1. Dirección configura la institución y vincula a sus docentes.
2. ProfeCuaderno crea las clases y entrega los códigos de vinculación.
3. El estudiante se une con su código y consulta únicamente su propia información académica.
4. Dirección mantiene la coordinación institucional y el horario sin asumir el rol académico del docente.

Sitio público del ecosistema: https://profecuaderno-api-production.up.railway.app/

Política de privacidad conjunta: https://profecuaderno-api-production.up.railway.app/privacy

## Compilación

El workflow `.github/workflows/build-apk.yml` ejecuta lint, genera APK/AAB de prueba y actualiza una descarga pública estable del APK cuando cambia `main`.

## Privacidad

El acceso está autenticado y las operaciones se validan por rol e institución en el backend. Antes de una publicación institucional definitiva deben confirmarse el responsable legal del tratamiento, un contacto estable de privacidad, los plazos de conservación y los proveedores de infraestructura vigentes. La política conjunta enlazada arriba describe el alcance actual de las tres aplicaciones online.
