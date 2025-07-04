# 🎮 Tic Tac Toe Game - Versión Mejorada

Una aplicación móvil completa de Tic Tac Toe desarrollada en **Kotlin** con **Jetpack Compose**, que ofrece múltiples modos de juego y efectos visuales espectaculares.

## ✨ Características Principales

### 🎯 Pantalla de Bienvenida
- **Diseño atractivo** con animaciones fluidas
- **Selección intuitiva** de modo de juego al iniciar la aplicación
- **Interfaz moderna** con gradientes y efectos visuales

### 🎮 Modos de Juego

#### 🤖 Single Player (Contra la IA)
- Juega contra una **inteligencia artificial inteligente**
- Algoritmo de IA que usa estrategias ganadoras
- Dificultad balanceada para una experiencia desafiante

#### 👥 Multijugador Local
- Juega con un amigo en el **mismo dispositivo**
- Turnos alternados entre jugadores X y O
- Perfecto para partidas rápidas

#### 📱 Multijugador Bluetooth
- Juega con amigos a través de **conexión Bluetooth**
- **Flujo completo de configuración** paso a paso:
  1. **Solicitud de permisos** automática
  2. **Ingreso del nombre** del jugador
  3. **Opciones de conexión**:
     - 🔍 Buscar dispositivos cercanos
     - 👁️ Hacerse visible para otros

### 🎆 Efectos Visuales Espectaculares

#### 🎉 Fuegos Artificiales para el Ganador
- **Animación de fuegos artificiales** cuando alguien gana
- **Múltiples colores vibrantes** y efectos de partículas
- **Mensaje de felicitación personalizado**
- **Animación fluida** que dura 8 segundos
- **Efectos de desvanecimiento** y gravedad realista

### 🔧 Funcionalidades Técnicas

#### 🔐 Gestión de Permisos Bluetooth
- **Solicitud automática** de permisos necesarios
- **Compatibilidad con Android 12+** (nuevos permisos Bluetooth)
- **Retrocompatibilidad** con versiones anteriores
- **Manejo de errores** y mensajes informativos

#### 📡 Comunicación Bluetooth Robusta
- **Descubrimiento automático** de dispositivos
- **Conexión estable** y manejo de reconexiones
- **Protocolo de comunicación** personalizado para movimientos
- **Estados de conexión** claros y visuales

#### 🎨 Interfaz de Usuario Moderna
- **Material Design 3** con temas adaptativos
- **Animaciones suaves** y transiciones fluidas
- **Navegación intuitiva** entre pantallas
- **Feedback visual** para todas las acciones

## 🚀 Flujo de Uso

### 1. Inicio de la Aplicación
```
🎮 Pantalla de Bienvenida
   ↓
🎯 Selección de Modo de Juego
   ↓
🎲 Inicio del Juego
```

### 2. Modo Bluetooth (Flujo Completo)
```
📱 Seleccionar "Multijugador Bluetooth"
   ↓
🔒 Solicitar Permisos Necesarios
   ↓
👤 Ingresar Nombre del Jugador
   ↓
🔄 Elegir Modo de Conexión:
   ├── 🔍 Buscar Dispositivos
   └── 👁️ Hacerse Visible
   ↓
📡 Establecer Conexión
   ↓
🎮 ¡Comenzar a Jugar!
```

### 3. Victoria y Celebración
```
🎯 Movimiento Ganador
   ↓
🎆 Animación de Fuegos Artificiales
   ↓
🏆 Mensaje de Felicitación
   ↓
🔄 Opción de Nuevo Juego
```

## 🛠️ Tecnologías Utilizadas

- **Kotlin**: Lenguaje de programación principal
- **Jetpack Compose**: Framework de UI moderno
- **Bluetooth Classic**: Comunicación entre dispositivos
- **Corrutinas**: Programación asíncrona
- **StateFlow**: Gestión de estado reactiva
- **Material Design 3**: Diseño y componentes UI

## 📋 Permisos Requeridos

### Para Android 12+ (API 31+)
- `BLUETOOTH_CONNECT`: Conectar con dispositivos
- `BLUETOOTH_SCAN`: Buscar dispositivos
- `BLUETOOTH_ADVERTISE`: Hacerse visible
- `ACCESS_FINE_LOCATION`: Requerido por Android

### Para Android 11 y anteriores
- `BLUETOOTH`: Funcionalidad básica
- `BLUETOOTH_ADMIN`: Administración Bluetooth
- `ACCESS_FINE_LOCATION`: Localización precisa
- `ACCESS_COARSE_LOCATION`: Localización aproximada

## 🎨 Características de Diseño

### Colores y Temas
- **Esquema de colores adaptativos** según el tema del sistema
- **Gradientes suaves** para fondos
- **Colores vibrantes** para efectos especiales
- **Alto contraste** para legibilidad

### Animaciones
- **Transiciones de pantalla** fluidas
- **Efectos de hover** en botones
- **Animaciones de entrada** con transparencia
- **Fuegos artificiales** con física realista

### Iconografía
- **Iconos Material Design** consistentes
- **Emojis** para mayor expresividad
- **Indicadores visuales** de estado
- **Iconografía intuitiva** para acciones

## 🔧 Instalación y Uso

1. **Clona el repositorio**
   ```bash
   git clone [URL_DEL_REPOSITORIO]
   ```

2. **Abre en Android Studio**
   - Android Studio Arctic Fox o superior
   - SDK mínimo: Android 7.0 (API 24)

3. **Compila y ejecuta**
   ```bash
   ./gradlew build
   ./gradlew installDebug
   ```

4. **¡Disfruta jugando!**

## 🎯 Próximas Funcionalidades

- 🌐 **Multijugador online** con servidor
- 📊 **Estadísticas** y historial de partidas
- 🎵 **Efectos de sonido** y música
- 🏅 **Sistema de logros** y puntuaciones
- 🎨 **Temas personalizables** adicionales
- 🤖 **Diferentes niveles de IA**

## 🐛 Reportar Problemas

Si encuentras algún problema o tienes sugerencias:
1. Abre un **Issue** en GitHub
2. Describe el problema detalladamente
3. Incluye capturas de pantalla si es posible
4. Especifica el modelo de dispositivo y versión de Android

## 📄 Licencia

Este proyecto está bajo la Licencia MIT. Ver el archivo `LICENSE` para más detalles.

---

**¡Desarrollado con ❤️ en Kotlin y Jetpack Compose!**

*Una experiencia de juego moderna, intuitiva y divertida para todos.* 