# Archivum

**Gestión y mantenimiento del archivo documental de una hermandad**

---

## 📖 Sobre el proyecto

Archivum es una aplicación de escritorio diseñada para que el secretario de una hermandad religiosa pueda digitalizar, catalogar, buscar y preservar todos los documentos históricos y administrativos.

Nace de un problema real: documentos centenarios en papel dispersos por estanterías, archivadores y cajones, deteriorándose con cada manipulación y sin un sistema para encontrarlos rápidamente.

## ✨ Funcionalidades

- 📄 **Subir documentos** con metadatos (título, tipo, fecha, descripción, ubicación física)
- 🔍 **Buscar** por texto, tipo, rango de fechas o año aproximado
- 👁️ **Ver detalle** y abrir archivos con el programa del sistema
- ✏️ **Editar** metadatos y reemplazar archivos
- 🗑 **Papelera** con restauración y eliminación definitiva
- 💾 **Backup y restauración** completa (base de datos + archivos)

## 🛠 Tecnologías

| Capa | Tecnología |
|------|------------|
| Lenguaje | Java 17 |
| Framework | Spring Boot 3.2 |
| Base de datos | H2 (archivo local) |
| Interfaz gráfica | JavaFX 21 |
| Build | Maven |

## 🚀 Cómo ejecutar

### Requisitos
- Java 17 o superior
- Maven 3.9 o superior

### Ejecutar en desarrollo
```bash
mvn clean compile
mvn spring-boot:run
```

### Empaquetar como JAR ejecutable
```bash
mvn clean package -DskipTests
java -jar target/archivum-1.0.0.jar
```

### 📁 Estructura del proyecto

archivum/
├── src/main/java/com/archivum/
│   ├── ArchivumApplication.java
│   ├── model/          # Entidad Documento y TipoDocumento
│   ├── repository/     # Acceso a datos
│   ├── service/        # Lógica de negocio y backup
│   └── ui/             # Ventanas JavaFX
├── src/main/resources/
│   ├── application.properties
│   └── estilo.css
├── data/               # Base de datos H2 (generada)
├── archivos/           # Documentos digitales
├── backups/            # Copias de seguridad

### 📸 Capturas de pantalla



### 👤 Autor
Enrique Cala Rodríguez

    GitHub: ecalar

    LinkedIn: Enrique Cala Rodríguez

    Portfolio: ecalar.github.io

### 📄 Licencia

Este proyecto es de uso personal para portfolio. Todos los derechos reservados.



