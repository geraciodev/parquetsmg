# ParquetsMG 📊

**ParquetsMG** es un visor de archivos Parquet moderno, rápido y multiplataforma diseñado para desarrolladores y analistas de datos. Utiliza **DuckDB** como motor analítico para ofrecer un rendimiento excepcional incluso con archivos de gran tamaño.

![Visor Parquet](https://img.shields.io/badge/UI-Compose_Multiplatform-blue)
![Engine](https://img.shields.io/badge/Engine-DuckDB-orange)
![Theme](https://img.shields.io/badge/Design-Material3_Dark-black)

## ✨ Características

- **🚀 Motor DuckDB:** Consultas ultrarrápidas y manejo eficiente de memoria.
- **🎨 UI Moderna:** Interfaz en modo oscuro con estética rectangular (sin bordes redondeados) para un look profesional y limpio.
- **🔍 Búsqueda y Filtrado:**
  - Búsqueda global en todas las columnas.
  - Filtros específicos por columna con selección de valores únicos.
- **📊 Estadísticas Avanzadas:** Calcula recuentos, valores únicos, promedios y sumas totales de cualquier columna al instante.
- **📋 Selector de Columnas:** Oculta o muestra columnas según tus necesidades.
- **📥 Exportación:** Exporta tus datos filtrados y seleccionados directamente a formato **CSV**.
- **📂 Asociación de Archivos:** Soporte para abrir archivos `.parquet` directamente desde el explorador de archivos.

## 🛠️ Stack Tecnológico

- **Lenguaje:** [Kotlin](https://kotlinlang.org/)
- **UI Framework:** [Compose Multiplatform](https://www.jetbrains.com/lp/compose-multiplatform/) (Material3)
- **Base de Datos:** [DuckDB](https://duckdb.org/) (vía JDBC)
- **Arquitectura:** Kotlin Multiplatform (KMP)

## 🚀 Cómo empezar

### Requisitos
- Java JDK 17 o superior.

### Ejecución en desarrollo
Para ejecutar la aplicación de escritorio:
```bash
./gradlew :desktopApp:run
```

Para activar el **Hot Reload** (recarga en vivo):
```bash
./gradlew :desktopApp:hotRun --auto
```

### Empaquetado
Para generar los instaladores nativos (MSI, DEB, DMG):
```bash
./gradlew :desktopApp:package
```
Los archivos generados se encontrarán en `desktopApp/build/compose/binaries`.

## 📂 Estructura del Proyecto

- `:shared`: Contiene la lógica de negocio, el servicio de DuckDB (`ParquetService`) y la UI compartida en Compose.
- `:desktopApp`: El punto de entrada para la aplicación de escritorio y la configuración de empaquetado nativo.

---

Desarrollado con ❤️ usando **Compose Multiplatform**.
