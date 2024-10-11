"use strict";(()=>{function o(t,r){return t[site.quantity(r)]||t.other||t.one||"no plural found"}function p(t){let r=(n,e)=>l(o(t,n),e).join("");return r.asArray=(n,e)=>l(o(t,n),e),r}function s(t){let r=n=>l(t,n).join("");return r.asArray=n=>l(t,n),r}function l(t,r){let n=t.split(/(%(?:d$)?s)/g);if(r.length){let e=n.indexOf("%s");if(e!==-1)n[e]=r[0];else for(let i=0;i<r.length;i++){let s=n.indexOf("%"+(i+1)+"$s");s!==-1&&(n[s]=r[i])}}return n}if(!window.i18n)window.i18n={};if(!window.i18n.puzzleTheme)window.i18n.puzzleTheme={};let i=window.i18n.puzzleTheme;i['advancedPawn']="Peón avanzado";i['advancedPawnDescription']="Uno de tus peones está bien avanzado en territorio enemigo, quizás amenazando promocionar.";i['advantage']="Ventaja";i['advantageDescription']="Aprovecha la oportunidad de obtener una ventaja decisiva. (200cp ≤ eval ≤ 600cp)";i['anastasiaMate']="Mate de Anastasia";i['anastasiaMateDescription']="Un caballo y una torre o dama se unen para atrapar al rey contrario entre un extremo del tablero y una pieza de su bando.";i['arabianMate']="Mate árabe";i['arabianMateDescription']="Un caballo y una torre se unen para atrapar al rey contrario en una esquina del tablero.";i['attackingF2F7']="Atacando f2 o f7";i['attackingF2F7Description']="Un ataque centrado en el peón f2 o f7, como en el ataque Fegatello.";i['attraction']="Atracción";i['attractionDescription']="Un cambio o sacrificio que invita o fuerza al oponente a colocar una pieza en una casilla que propicia una oportunidad táctica.";i['backRankMate']="Mate del pasillo";i['backRankMateDescription']="Jaque mate en la última fila, cuando el rey se encuentra atrapado por sus propias piezas.";i['bishopEndgame']="Final de alfiles";i['bishopEndgameDescription']="Un final con sólo alfiles y peones.";i['bodenMate']="Mate de Boden";i['bodenMateDescription']="Dos alfiles atacantes en diagonales cruzadas dan mate al rey, obstruido por piezas de su bando.";i['capturingDefender']="Capturar al defensor";i['capturingDefenderDescription']="Eliminar una pieza fundamental para la defensa de otra, permitiendo capturar la pieza, ahora indefensa, en el futuro.";i['castling']="Enroque";i['castlingDescription']="Lleva al rey a un lugar seguro y despliega la torre para atacar.";i['clearance']="Despeje";i['clearanceDescription']="Un movimiento, a menudo con ganancia de tiempo, que limpia una casilla, una fila o una diagonal para una idea táctica de seguimiento.";i['crushing']="Aplastante";i['crushingDescription']="Detecta el error del oponente para obtener una ventaja ganadora. (eval ≥ 600cp)";i['defensiveMove']="Movimiento defensivo";i['defensiveMoveDescription']="Un movimiento o una secuencia de movimientos necesarios para evitar la pérdida de material u otra ventaja.";i['deflection']="Desviación";i['deflectionDescription']="Un movimiento que distrae una pieza del oponente de alguna de las tareas que desempeña, como la protección de una casilla clave. También conocido como \\\"sobrecarga\\\".";i['discoveredAttack']="Ataque a la descubierta";i['discoveredAttackDescription']="Mover una pieza que bloqueaba el ataque de otra pieza de largo alcance, como por ejemplo, quitar un caballo del camino de una torre.";i['doubleBishopMate']="Mate de los dos alfiles";i['doubleBishopMateDescription']="Dos alfiles atacantes en diagonales adyacentes dan mate al rey obstruido por piezas de su bando.";i['doubleCheck']="Jaque doble";i['doubleCheckDescription']="Dar jaque con dos piezas a la vez, como consecuencia de una ataque a la descubierta, donde tanto la pieza movida como la revelada atacan al rey del oponente.";i['dovetailMate']="Mate de Cozio";i['dovetailMateDescription']="Una dama da mate al rey adyacente, cuyas únicas dos casillas de escape están obstruidas por piezas de su bando.";i['endgame']="Final";i['endgameDescription']="Una táctica durante la última fase de la partida.";i['enPassantDescription']="Una táctica que involucra la captura al paso, donde un peón puede capturar a un peón oponente que lo ha pasado por alto usando su movimiento inicial de dos casillas.";i['equality']="Igualdad";i['equalityDescription']="Recupérate de una posición perdedora y asegura las tablas o una posición de igualdad. (eval ≤ 200cp)";i['exposedKing']="Rey expuesto";i['exposedKingDescription']="Una táctica que involucra a un rey con poca defensa a su alrededor, a menudo conduce a jaque mate.";i['fork']="Ataque doble";i['forkDescription']="Un movimiento donde la pieza movida ataca dos piezas del oponente a la vez.";i['hangingPiece']="Pieza colgada";i['hangingPieceDescription']="Una táctica que implica que una pieza del adversario no esté defendida o insuficientemente defendida y quede para ser capturada.";i['healthyMix']="Mezcla equilibrada";i['healthyMixDescription']="Un poco de todo. No sabes lo que te espera, así que estate listo para cualquier cosa, como en las partidas reales.";i['hookMate']="Mate del gancho";i['hookMateDescription']="Jaque mate con una torre, un caballo y un peón junto con un peón enemigo para limitar el escape del rey adversario.";i['interference']="Interferencia";i['interferenceDescription']="Mover una pieza entre dos piezas del oponente para dejar una o ambas piezas del oponente sin defensa, por ejemplo un caballo en una casilla entre dos torres.";i['intermezzo']="Jugada intermedia";i['intermezzoDescription']="En lugar de jugar el movimiento esperado, se realiza antes otro movimiento que plantea una amenaza inmediata a la que el oponente debe responder. También conocido como \\\"Zwischenzug\\\" o \\\"Intermezzo\\\".";i['kingsideAttack']="Ataque en el flanco de rey";i['kingsideAttackDescription']="Un ataque contra el enroque corto.";i['knightEndgame']="Final de caballos";i['knightEndgameDescription']="Un final solo con caballos y peones.";i['long']="Ejercicio largo";i['longDescription']="Tres movimientos para ganar.";i['master']="Partidas de maestros";i['masterDescription']="Ejercicios de partidas de jugadores titulados.";i['masterVsMaster']="Partidas entre maestros";i['masterVsMasterDescription']="Ejercicios de partidas entre jugadores titulados.";i['mate']="Jaque mate";i['mateDescription']="Gana la partida con estilo.";i['mateIn1']="Mate en 1";i['mateIn1Description']="Dar mate en un movimiento.";i['mateIn2']="Mate en 2";i['mateIn2Description']="Dar mate en dos movimientos.";i['mateIn3']="Mate en 3";i['mateIn3Description']="Dar mate en tres movimientos.";i['mateIn4']="Mate en 4";i['mateIn4Description']="Dar mate en cuatro movimientos.";i['mateIn5']="Mate en 5 o más";i['mateIn5Description']="Calcular una secuencia de mate larga.";i['middlegame']="Medio juego";i['middlegameDescription']="Una táctica durante la segunda fase de la partida.";i['oneMove']="Ejercicio de un solo movimiento";i['oneMoveDescription']="Un movimiento para ganar.";i['opening']="Apertura";i['openingDescription']="Una táctica durante la primera fase de la partida.";i['pawnEndgame']="Final de peones";i['pawnEndgameDescription']="Un final solo con peones.";i['pin']="Clavada";i['pinDescription']="Una táctica donde una pieza no puede moverse sin revelar un ataque a una pieza de mayor valor.";i['playerGames']="Partidas de jugadores";i['playerGamesDescription']="Busca ejercicios generados a partir de tus partidas o de las de otros jugadores.";i['promotion']="Promoción";i['promotionDescription']="Promover uno de sus peones a una dama o una pieza menor.";i['puzzleDownloadInformation']=s("Estos ejercicios son de dominio público y pueden descargarse desde %s.");i['queenEndgame']="Final de damas";i['queenEndgameDescription']="Un final solo con damas y peones.";i['queenRookEndgame']="Final de dama y torre";i['queenRookEndgameDescription']="Un final solo con damas, torres y peones.";i['queensideAttack']="Ataque en el flanco de dama";i['queensideAttackDescription']="Un ataque al rey del oponente, luego de haber realizado el enroque largo.";i['quietMove']="Jugada tranquila";i['quietMoveDescription']="Un movimiento que no da jaque o captura pieza, ni amenza inmediatamente capturar una pieza, pero que prepara una amenaza inevitable más oculta para una jugada posterior.";i['rookEndgame']="Final de torres";i['rookEndgameDescription']="Un final solo con torres y peones.";i['sacrifice']="Sacrificio";i['sacrificeDescription']="Una táctica que implica ceder material a corto plazo, para obtener una ventaja luego de una secuencia forzada de movimientos.";i['short']="Ejercicio corto";i['shortDescription']="Dos movimientos para ganar.";i['skewer']="Pincho";i['skewerDescription']="Un motivo que implica el ataque de una pieza de alto valor, que se aparta del camino y permite capturar o atacar una pieza de menor valor detrás de ella, lo contrario de una clavada.";i['smotheredMate']="Mate de la coz";i['smotheredMateDescription']="Un jaque mate con caballo, en el cual el rey enemigo no se puede mover al encontrarse rodeado (o ahogado) por sus propias piezas.";i['superGM']="Partidas de súper grandes maestros";i['superGMDescription']="Ejercicios de partidas de los mejores jugadores del mundo.";i['trappedPiece']="Pieza atrapada";i['trappedPieceDescription']="Una pieza no puede escapar de la captura porque tiene movimientos limitados.";i['underPromotion']="Subpromoción";i['underPromotionDescription']="Promoción a caballo, alfil o torre.";i['veryLong']="Ejercicio muy largo";i['veryLongDescription']="Cuatro movimientos o más para ganar.";i['xRayAttack']="Ataque por rayos X";i['xRayAttackDescription']="Una pieza ataca o defiende una casilla, a través de una pieza del oponente.";i['zugzwang']="Zugzwang";i['zugzwangDescription']="El oponente está limitado en los movimientos que puede realizar, y todos los movimientos empeoran su posición."})()