# FNAF RTS v0.1.2
## 1. Sistemas de juego 
### 1.1 Puertas y calor (DoorSystem) 
Recurso compartido que sube con puertas cerradas y baja con puertas abiertas. 
Estado  Velocidad 
- Ambas abiertas  −1.75/s 
- Una cerrada  +1.00/s 
- Ambas cerradas  +2.25/s 
- Enfriando (sobrecalentado)  −0.75/s 
Al llegar a MAX_HEAT = 40.0 entra en OVERHEATED: ambas puertas se fuerzan a abrir y el jugador no puede operarlas hasta que el calor llegue a 0. 
También expone: 
- toggleLeft/Right — ignora si la puerta está bloqueada o el sistema 
sobrecalentado. 
- raiseHeat(amount) — para eventos puntuales (shock de Endo sube +5). 
- setLeftBlocked/RightBlocked — para que Chica y Bonnie reserven una puerta. 
### 1.2 Atención (AttentionSystem) 
Recurso alternativo a la batería clásica. Modelo de "tasa por frame": 
- Luces: 0 luces → −1.00/s, 1 luz → +0.75/s, 2 luces → +1.50/s. 
- Aportes continuos (contribute(rate)) de animatrónicos: se acumulan y 
sobrescriben el decay base. 
- Subidas puntuales (raise(amount)) de eventos: pizza (+3), shock (+5), Foxy 
bloqueado (+5), Freddy en fase de ruido (+0.25/s). 
Si el total del frame es 0, aplica decay −1.00/s. Satura en 100. 
### 1.3 Cámaras (GameState) 
- Cambio de sector con cooldown de 1500 ms. 
- Input buffer de 250 ms: si el jugador clickea durante el cooldown y faltan ≤250 
ms, la intención se guarda y se ejecuta automáticamente al terminar. 
- Cámaras bloqueables (setCameraBlocked): Bonnie en H16 bloquea hallway1 y 
hallway2. Si el jugador las está mirando, se lo expulsa a null. 
- Apagar cámaras (turnOffCameras): deja activeSectorId = null. Necesario contra 
Golden Freddy. 
### 1.4 Última vez visto 
Cada animatrónico guarda lastSeenNodeId. GameState.updateLastSeen() corre cada tick: 
- Si el animatrónico es visible para el jugador → marca su nodo actual. 
- Si no es visible, pero la marca apunta a una zona visible → borra la marca 
(re-escaneaste y no está). 
- Si no es visible y la marca apunta a zona no visible → la deja (sigue siendo 
útil). 
"Visible" se decide en isNodeVisibleToPlayer: 
- OFFICE → siempre. 
- ENTRY_LEFT/RIGHT → si su luz está encendida. 
- Resto → si su sector es el activo. 
- Además, cualquier animatrónico con isForcedVisible() == true se considera 
visible. 
 
## 2. Los 6 animatrónicos 
Todos heredan de Animatronic y sobreescriben tick(dt, GameState). 
### 2.1 Chica 
- Home: Escenario. 
- Ruta: Al salir del Escenario se va hacia el comedor y luego la cocina. Allí se queda un rato haciendo ruido hasta que se cansa y va hacia tu oficina por el pasillo de abajo.
- Pizza: si el jugador la ve en la cocina, puede ordenar pizza a cambio de prestar atención. Chica come la pizza durante un rato, luego, ya alimentada, sube su velocidad ligeramente. 
- Puerta: espera un rato en la oscuridad, si no le cierras la puerta entra para escanearte y una vez identificado te matá. 
- Observación: Se mueve más lento si la miras. 
- Velocidad: Mediana. 
### 2.2 Bonnie 
- Home: Escenario. 
- Ruta: Al salir del escenario va rapidamente por arriba hacia tu puerta derecha. Es bastante agil, no esperes que se bloqueé con otros animatronicos.
- Desvío: A veces se cansa de ser bloqueado y en vez de eso se queda un rato al final del pasillo superior jugando con los cables. 
- Puerta: espera un rato en la oscuridad, si no le cierras la puerta entra para escanearte y una vez identificado te matá. 
- Observación: Se mueve más lento si lo miras. 
- Velocidad: Rapida. 
### 2.3 Freddy 
- Home: Escenario. 
- Ruta: Elije cualquiera de los dos caminos. 
- Puerta: Cuando llega a tu puerta se pone a reproducir música, luego se prepara para matarte, no lo veas mucho. 
- Observación: no avanza mientras lo estes viendo. 
- Velocidad: Lento. 
### 2.4 Foxy 
- Home: Entrada. 
- Ruta: Recorre la Entrada y una vez que llega al final pegá un acelerón hacia tu oficina. 
- Observación: No le gusta que lo miren, tarda más en moverse. 
- Puerta: Te mata automaticamente si te atrapa con la puerta abierta. Si está cerrada toca la puerta y se va. 
- Velocidad: Muy Rapido
### 2.5 Golden Freddy 
- Home: Back Stage. 
- Teletransporte: Cada cierto tiempo se teletransporta a un lugar aleatorio (no 
entrada, no sala de empleados, no ENTRY). 
- Oficina: Si se teletransporta a la oficina apaga las camaras automaticamente, nadie te debería ver allí. 
- Camaras: No le gusta que lo miren demasiado. Como no le afectan las leyes de la física, en vez de paralizarse, te matá.
- Velocidad: Muy lento
### 2.6 Endo 
- Home: Sala de Empleados. 
- Wandering: Deambula por la sala de empleados, una vez que se pone en la linea de tu puerta esta listo par ir a tu puerta izquierda. 
- Shock: Si llega a ese punto no temas usar el Shock electrico. Reinicia sus sistemas y vuelve a su posición de inicio. 
- Puerta: Puede llegar a tu oficina en dos modos dependiendo de donde partió. 
    - Silencioso: Va disrecto a tu puerta y te mata en cuestión de segundos.
    - Rapido: Se mueve a gran velocidad y espera un poco más. 
- Respawn: Dependiendo del modo en el que fue bloqueado puede volver a su estado inicial o irse afuera. Si se va afuera va a estar volviendo a tu puerta izquierda mucho más rapido y no podrá shockarse. 
- Luz: Evita usar la luz de las puertas con el, no le gustará nada y te matara automaticamente. 
 
## 3. Interfaz 
Layout del MainFrame 
text 

    ┌─────────────────────────────────────────┐                                                                             
    │ HUDPanel: reloj │ atención │ calor      │                                                                             
    ├─────────────────────────────────────────┤                                                                             
    │                                         │                                                                             
    │              MapPanel                   │                                                                             
    │       (grafo + animatrónicos)           │                                                                             
    │                                         │                                                                             
    ├─────────────────────────────────────────┤                                                                             
    │ OfficePanel: widgets de puertas + botones│                                                                             
    └─────────────────────────────────────────┘                                                                             
#### MapPanel 
- Rejilla adaptativa: calcula bounding box de todos los nodos y escala. 
- Nodos cuadrados con sombra, borde y highlight. 
- Sectores con fondo redondeado, borde pulsante si es el activo. 
- Links con color según si tocan el sector activo. 
- Barra de puerta: línea gruesa verde/roja entre cada ENTRY y OFFICE. 
- Estática durante 320 ms al cambiar de cámara (con scanline descendente). 
- Cooldown ring: círculo pastel en la esquina superior derecha. 
- Hover: borde azulado al pasar el mouse sobre un sector. 
- Last seen: badge translúcido con borde punteado en el último nodo visto. 
#### HUDPanel 
- Reloj en formato H:MM calculado desde elapsedSeconds. 
- Barra de atención con color según rango (cyan / violeta / magenta). 
- Barra de calor con color según rango (verde → amarillo → naranja → rojo). 
#### OfficePanel 
- Widget de puerta izquierda + 3 slots de acción + widget de puerta derecha. 
- Pizza (W): visible solo en cocina, habilitado solo si Chica puede aceptar. 
- Shock (S): visible solo en sala de empleados, habilitado solo si Endo puede ser shockeado. 
- Cámaras (C): visible siempre que haya cámara activa. 
#### DoorWidget 
- Estado de puerta: ABIERTA / CERRADA / SOBRECALENTADA / BLOQUEADA (morado). 
- Estado de luz: APAGADA / ENCENDIDA. 
- Botones PUERTA y LUZ con atajos y hover. 
#### GameOverOverlay 
- Glass pane con overlay negro al 68%. 
- Título grande: GAME OVER o 6 AM. 
- Subtítulo con razón de muerte. 
- Hint: "Presioná R para reiniciar la noche · ESC para volver al menú". 
 
## 4. Atajos de teclado 
Tecla  Acción:
- Q  Toggle puerta izquierda.
- E  Toggle puerta derecha.
- A  Toggle luz izquierda.
- D  Toggle luz derecha.
- W  Ordenar pizza (en cocina).
- S  Shockear a Endo (en sala de empleados).
- C  Apagar cámaras.
- R  Reiniciar noche (solo en game over).
- ESC  Volver al menú (solo en game over).
 
## 5. Decisiones técnicas clave 
Concurrencia 

Un solo GameLoop con ScheduledExecutorService a 10 Hz (100 ms/tick). 
Tick único en GameState: tickea puertas, animatrónicos, atención y última vez 
visto dentro del mismo synchronized (lock). Esto elimina condiciones de carrera 
sin locks distribuidos. 

activeSectorId es volatile: escrito por el EDT (clicks), leído por el hilo del loop. 
La vista nunca muta el estado: usa AnimatronicView (record inmutable) y 
getters que devuelven copias. 

BFS cacheado 

MapGraph.shortestPath usa un HashMap<String, List<String>> con key from>to (o 
from>to!avoid1,avoid2). El grafo es estático, así que el cache se llena en las primeras 
iteraciones y se reutiliza toda la partida. 

Última vez visto 

No se calcula en la vista. El GameState.updateLastSeen() centraliza la lógica y la vista 
solo lee. Esto evita tener dos definiciones distintas de "visible". 

Input buffering 

Cuando el jugador clickea durante el cooldown de cámara, si faltan ≤250 ms se 
guarda la intención en bufferedSectorId. Se consume en el próximo tick cuando el 
cooldown expira. Cambia la sensación de respuesta sin tocar la lógica del cooldown. 

Bloqueo de puertas 

Chica y Bonnie exponen getBlockedDoorNodeId(). GameState.tick recalcula cada frame 
qué puertas están bloqueadas. DoorSystem.toggle ignora clicks si la puerta está 
bloqueada. La vista solo pinta el estado. 

Tema centralizado 

Todos los colores y fuentes viven en Theme.java. Cambiar un color se reduce a editar 
una constante. 
 
## 6. Estado actual y pendientes 
Completo 
- Modelo de grafo con parser de mapa. 
- Sistema de puertas con calor. 
- Sistema de atención. 
- Cámaras con cooldown, input buffer, bloqueo y apagón. 
- Los 6 animatrónicos con IA única. 
- UI completa: mapa, HUD, oficina, menú, game over. 
- Efectos visuales: estática, pulso de sectores, hover, blink. 
- Last seen. 
- Reinicio de noche y vuelta al menú. 
#### Pendiente 
- AI level por hora: los animatrónicos son igual de agresivos a las 12 AM que a 
las 5 AM. Falta escalar moveInterval y/o aiLevel con la hora. 
- Panel de log visible: el log existe en GameState.eventLog pero no se muestra 
en la UI. 
- Sonidos: pasos, golpes, motor, jumpscare, 6 AM. 
- Jumpscare animado: pantalla completa con zoom. 
- Balance fino: jugar partidas completas y ajustar números. 
- Modo debug: semilla visible, mostrar todos los animatrónicos siempre, 
acelerar el tiempo.
 
## 7. Cómo correrlo 
bash 
- mvn clean compile 
mvn exec:java 
En VSC, usar la extensión Maven for Java y ejecutar la meta exec:java. No usar el 
botón de Run del editor porque no copia recursos al classpath. 
En NetBeans, importar el proyecto como Maven existente y correr. 

### Licencia
Proyecto académico. Sin licencia específica. Los assets visuales son geométricos y generados por código. Los nombres de los animatrónicos son referencias culturales y no pretenden infringir derechos de autor.

### Créditos
Desarrollado por Luka Luraschi como trabajo práctico de la universidad.

Inspirado en Five Nights at Freddy's de Scott Cawthon.