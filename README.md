# VetGo

Aplicación móvil Android (Kotlin) para conectar tutores de mascotas con veterinarios cercanos. **VetGo** muestra profesionales/clinicas en un mapa interactivo, permite explorar servicios y prepara el terreno para agendar citas y flujos de registro/login. Esta es una **versión académica Beta** (implementación inicial en *Activities*; la versión final migrará a *Fragments*).

## Integrantes
- Juan Camilo Ríos Mesa

## Tecnologías principales
- Android Studio (Kotlin, AGP 8.x)
- Google Maps SDK for Android
- Firebase (Google Services / Analytics; Auth en siguiente iteración)

## Requisitos
- Android Studio Iguana (o superior)
- JDK 17
- Dispositivo/emulador con Google Play Services
- Claves propias de Google (Maps/Firebase)

## Configuración rápida
1. Clona este repositorio y ábrelo en Android Studio.
2. En el **módulo `app/`**, coloca tu archivo **`google-services.json`** (no se versiona).
3. Verifica el plugin en `app/build.gradle.kts`:
   ```kotlin
   plugins {
       id("com.android.application")
       id("org.jetbrains.kotlin.android")
       id("com.google.gms.google-services") // requerido por Firebase
   }
   dependencies {
       implementation(platform("com.google.firebase:firebase-bom:33.0.0"))
       implementation("com.google.firebase:firebase-analytics-ktx") // o el SDK de Firebase que uses
   }
   ```
4. Configura la **API Key de Google Maps** en:
   - `app/src/debug/res/values/google_maps_api.xml`
   - `app/src/release/res/values/google_maps_api.xml`
   Dentro del archivo, asigna tu clave a:
   ```xml
   <string name="google_maps_key" translatable="false">TU_API_KEY</string>
   ```
5. Sincroniza Gradle: **File → Sync Project with Gradle Files**.

## Ejecución
- Selecciona un dispositivo/emulador y pulsa **Run**.
- El módulo `app` contiene el *Splash/Login* (beta) y **`Maps_Activity`** para visualizar el mapa listo para personalización.

## Estructura (resumen)
```
app/
 ├─ build.gradle.kts
 ├─ google-services.json       (local, no se sube a Git)
 └─ src/main/
    ├─ AndroidManifest.xml
    ├─ java|kotlin/com/example/vetgobeta/...
    └─ res/...
```

## Troubleshooting
- **`Default FirebaseApp is not initialized...`**
  - Asegúrate de que el `applicationId` en `app/build.gradle.kts` coincida con `package_name` en `google-services.json`.
  - Verifica que el archivo se llame **exactamente** `google-services.json` y esté en `app/`.
  - Incluye al menos un SDK de Firebase en `dependencies` y sincroniza.
  - Ejecuta **Build → Clean Project** y luego **Build → Rebuild Project**.
- **Mapa en blanco**
  - Revisa la API Key, restricciones y que el dispositivo tenga Google Play Services.

## Estado del proyecto
- ✅ Estructura base Kotlin (Activities)
- ✅ Integración inicial de Google Maps
- ✅ Configuración de Firebase iniciada
- ⏳ Login con Firebase Auth
- ⏳ CRUD de clínicas/servicios + marcadores dinámicos
- ⏳ Navegación y UI con Fragments (versión definitiva)

## Licencia
Proyecto académico — uso educativo.
