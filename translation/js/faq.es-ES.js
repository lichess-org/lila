"use strict";(()=>{function o(t,r){return t[site.quantity(r)]||t.other||t.one||"no plural found"}function p(t){let r=(n,e)=>l(o(t,n),e).join("");return r.asArray=(n,e)=>l(o(t,n),e),r}function s(t){let r=n=>l(t,n).join("");return r.asArray=n=>l(t,n),r}function l(t,r){let n=t.split(/(%(?:d$)?s)/g);if(r.length){let e=n.indexOf("%s");if(e!==-1)n[e]=r[0];else for(let i=0;i<r.length;i++){let s=n.indexOf("%"+(i+1)+"$s");s!==-1&&(n[s]=r[i])}}return n}if(!window.i18n)window.i18n={};if(!window.i18n.faq)window.i18n.faq={};let i=window.i18n.faq;i['accounts']="Cuentas";i['acplExplanation']="El centipeón es la unidad de medida utilizada en el ajedrez como representación de la ventaja. Un centipeón es igual a una centésima parte de un peón. Por lo tanto, 100 centipeones = 1 peón. Estos valores no juegan ningún papel formal en el juego, pero son útiles para los jugadores y esenciales en el ajedrez por ordenador para evaluar las posiciones.\n\nEl mejor movimiento del ordenador perderá cero centipeones, pero los movimientos inferiores conllevarán un deterioro de la posición medido en centipeones.\n\nEste valor puede ser usado como un indicador de la fuerza de juego. Cuantos menos centipeones se pierdan por movimiento, más fuerte es el juego.\n\nEl análisis informático en Lichess lo proporciona Stockfish.";i['adviceOnMitigatingAddiction']=s("Solemos recibir mensajes de gente que nos pide ayuda para no jugar tanto.\n\nAunque Lichess no prohibe ni bloquea a los jugadores, excepto si se trata de violaciones de los Terminos de Servicio, recomendamos el uso de herramientas de terceros para limitar el comportamiento de juego excesivo. Algunas sugerencias de bloqueadores de sitios web son %1$s, %2$s, y %3$s. Si lo que deseas es seguir usando el sitio, pero no los controles de tiempo más rápidos, te podría interesar %4$s y %5$s.\n\nAlgunos jugadores pueden pensar que su comportamiento de juego se acerca a una adicción. De hecho, la OMS califica como desorden de juego a %6$s, cuyas características principales son: 1) control deficiente sobre el juego, 2) aumento de la prioridad del juego, y 3) mayor aumento del juego, a pesar de consecuencias negativas. Si piensas que tu comportamiento en el juego de ajedrez se parece a este patrón, te aconsejamos que hables de ello con algún familiar o amigo, o con un profesional dedicado, si es necesario.");i['aHourlyBulletTournament']="un torneo Bullet por hora";i['areThereWebsitesBasedOnLichess']="¿Hay páginas web basadas en Lichess?";i['asWellAsManyNMtitles']="muchos títulos de maestros nacionales";i['basedOnGameDuration']=s("Los controles de tiempo de Lichess se basan en la duración estimada de una partida = %1$s\n Por ejemplo, la duración estimada de una partida de 5+3 es 5 * 60 + 40 * 3 = 420 segundos.");i['beingAPatron']="ser patrón";i['beInTopTen']="estar entre los 10 primeros de esta clasificación.";i['breakdownOfOurCosts']="desglose de nuestros gastos";i['canIbecomeLM']="¿Puedo obtener el título de Maestro Lichess (LM)?";i['canIChangeMyUsername']="¿Puedo cambiar mi nombre de usuario?";i['configure']="configurar";i['connexionLostCanIGetMyRatingBack']="He perdido un juego debido a un retraso/desconexión. ¿Puedo recuperar mis puntos?";i['desktop']="escritorio";i['discoveringEnPassant']="¿Por qué un peón puede capturar otro peón cuando ya está pasado? (en passant)";i['displayPreferences']="preferencias de visualización";i['durationFormula']="(tiempo inicial del reloj) + 40 × (incremento)";i['eightVariants']="otras 8 variantes";i['enableAutoplayForSoundsA']="La mayoría de los navegadores pueden evitar que se reproduzca sonido en una página recién cargada para proteger a los usuarios. Imagínate si todos los sitios web pudieran bombardearte inmediatamente con anuncios de audio.\n\nEl icono rojo de silencio aparece cuando tu navegador impide que lichess.org reproduzca un sonido. Por lo general, esta restricción se elimina una vez que haces clic en algo. En algunos navegadores móviles, tocar y arrastrar una pieza no cuenta como un clic. En ese caso debes tocar el tablero para permitir el sonido al inicio de cada juego.\n\nMostramos el icono rojo para avisarte cuando esto suceda. A menudo, puedes permitir explícitamente que lichess.org reproduzca sonidos. Aquí encontrarás instrucciones para hacerlo en versiones recientes de algunos navegadores populares.";i['enableAutoplayForSoundsChrome']="1. Ve a lichess.org\n2. Haz clic en el icono de bloqueo de la barra de direcciones\n3. Haz clic en Configuración del sitio\n4. Habilitar sonido";i['enableAutoplayForSoundsFirefox']="1. Ve a lichess.org\n2. Pulsa Ctrl-i en Linux/Windows o cmd-i en MacOS\n3. Haz clic en la pestaña Permisos\n4. Permitir audio y video en lichess.org";i['enableAutoplayForSoundsMicrosoftEdge']="1. Haz clic en los tres puntos de la esquina superior derecha\n2. Haz clic en Ajustes\n3. Haz clic en Cookies y permisos del sitio\n4. Desplázate hacia abajo y haz clic en Reproducción automática\n5. Añade a lichess.org en Habilitar";i['enableAutoplayForSoundsQ']="¿Habilitar la reproducción automática de sonidos?";i['enableAutoplayForSoundsSafari']="1. Ve a lichess.org\n2. Haz clic en Safari de la barra de menú\n3. Haz clic en Configuración de lichess.org ...\n4. Habilitar la reproducción automática";i['enableDisableNotificationPopUps']="¿Activar o desactivar las notificaciones emergentes?";i['enableZenMode']=s("Habilita el modo Zen en %1$s o presionando %2$s durante una partida.");i['explainingEnPassant']=s("Este movimiento es legal, y se llama \\\"captura al paso\\\". El artículo de Wikipedia da una %1$s al mismo.\n\nSe describe en la sección 3.7 (d) de las %2$s:\n\n\\\"Un peón que ocupa un cuadrado en la misma fila y en una columna adyacente al peón de un oponente que acaba de avanzar dos casillas en un movimiento de su cuadrado original puede capturar el peón de este oponente como si este solo hubiera sido movido una casilla. Esta captura sólo es legal en el movimiento inmediatamente posterior a este avance y se denomina captura “al paso”.\n\nMira el capítulo del %3$s sobre este movimiento para practicar un poco con él.");i['fairPlay']="Juego limpio";i['fairPlayPage']="página de juego limpio";i['faqAbbreviation']="Preguntas frecuentes";i['fewerLobbyPools']="menos casillas de emparejamiento";i['fideHandbook']="Manual de la FIDE";i['fideHandbookX']=s("%s del manual de la FIDE");i['findMoreAndSeeHowHelp']=s("Puedes aprender más sobre %1$s (incluyendo un %2$s). Si quieres ayudar a Lichess voluntariamente con tu tiempo y habilidades, hay muchas %3$s.");i['frequentlyAskedQuestions']="Preguntas frecuentes";i['gameplay']="Mecánica de juego";i['goldenZeeExplanation']="ZugAddict estaba transmitiendo y durante las dos últimas horas había estado tratando de derrotar a la I.A. nivel 8 en una partida 1+0, sin éxito. Thibault le dijo que si lo lograba durante la transmisión, obtendría un trofeo único. Una hora después, venció a Stockfish y se cumplió la promesa.";i['goodIntroduction']="buena introducción";i['guidelines']="pautas";i['havePlayedARatedGameAtLeastOneWeekAgo']="haber jugado una partida por puntos en la última semana con esta puntuación,";i['havePlayedMoreThanThirtyGamesInThatRating']="haber jugado al menos 30 partidas por puntos con una puntuación dada,";i['hearItPronouncedBySpecialist']="Así es como lo pronuncia un especialista.";i['howBulletBlitzEtcDecided']="¿Cómo se deciden los controles de tiempo en Bullet, Blitz y otros?";i['howCanIBecomeModerator']="¿Cómo puedo convertirme en moderador?";i['howCanIContributeToLichess']="¿Cómo puedo contribuir a Lichess?";i['howDoLeaderoardsWork']="¿Cómo funcionan los rankings y el listado de líderes?";i['howToHideRatingWhilePlaying']="¿Cómo ocultar mis puntuaciones durante las partidas?";i['howToThreeDots']="Cómo...";i['inferiorThanXsEqualYtimeControl']=s("< %1$s s = %2$s");i['inOrderToAppearsYouMust']=s("Para llegar al %1$s deberías:");i['insufficientMaterial']="Perder por tiempo, tablas y material insuficiente";i['isCorrespondenceDifferent']="¿El ajedrez por correspondencia es diferente del ajedrez normal?";i['keyboardShortcuts']="¿Qué atajos de teclado hay?";i['keyboardShortcutsExplanation']="Algunas páginas de Lichess tienen atajos de teclado que puedes utilizar. Presiona la tecla \\'?\\' en un estudio, análisis, ejercicio o en una partida para listar los atajos de teclado disponibles.";i['leavingGameWithoutResigningExplanation']="Si tu oponente abandona habitualmente partidas se le marca como \\\"prohibido jugar\\\", lo que significa que está temporalmente suspendido para jugar. Esto no se indica públicamente en su perfil. Si persiste en este comportamiento, la duración de la suspensión aumenta y puede conllevar el cierre de su cuenta.";i['leechess']="liches";i['lichessCanOptionnalySendPopUps']="Opcionalmente, Lichess puede enviar notificaciones emergentes, por ejemplo cuando sea tu turno o hayas recibido un mensaje privado.\n\nHaz clic en el icono de bloqueo junto a la dirección de lichess.org, en la barra de enlaces de tu navegador.\n\nLuego selecciona si permitir o bloquear notificaciones de Lichess.";i['lichessCombinationLiveLightLibrePronounced']=s("Lichess es una combinación de live/light/libre (en directo, ligero y libre, en inglés) y chess (ajedrez, en inglés). Se pronuncia %1$s.");i['lichessFollowFIDErules']=s("En el caso de que un jugador se quede sin tiempo, ese jugador normalmente perderá la partida. Sin embargo, el juego se considera empate si la posición es tal que el oponente no puede dar mate al rey del jugador por cualquier posible serie de movimientos legales (%1$s).\n\n  En raras ocasiones esto puede ser difícil de decidir automáticamente (líneas forzadas, fortalezas). Por defecto, siempre estamos de parte del jugador que no se quedó sin tiempo.\n\n  Ten en cuenta que es posible dar mate con un sólo caballo o alfil, siempre que el oponente tenga una pieza que pueda bloquear al rey.");i['lichessPoweredByDonationsAndVolunteers']="Lichess se mantiene mediante donaciones de patrocinadores y los esfuerzos de un equipo de voluntarios.";i['lichessRatings']="Puntuaciones de Lichess";i['lichessRecognizeAllOTBtitles']=s("Lichess reconoce todos los títulos FIDE obtenidos en torneos presenciales, así como %1$s. Aquí hay una lista de títulos FIDE:");i['lichessSupportChessAnd']=s("Lichess soporta ajedrez estándar y %1$s.");i['lichessTraining']="Entrenamiento en Lichess";i['lichessUserstyles']="Estilos de usuario de Lichess";i['lMtitleComesToYouDoNotRequestIt']="Este título honorífico no es oficial y sólo existe en Lichess.\n\nOcasionalmente otorgamos a jugadores muy destacados que son buenos ciudadanos de Lichess, a nuestro criterio. El título de LM no se consigue, sino que te llega. Si cumples los requisitos, recibirás un mensaje nuestro al respecto y la opción de aceptar o rechazar el título.\n\nNo solicites el título de LM.";i['mentalHealthCondition']="condición de salud mental independiente";i['notPlayedEnoughRatedGamesAgainstX']=s("El jugador aún no ha jugado suficientes partidas por puntos contra %1$s en la categoría.");i['notPlayedRecently']="El jugador no ha jugado suficientes partidas recientes. Dependiendo de la cantidad de partidas que hayas jugado, puede tardar alrededor de un año de inactividad para que tu puntuación vuelva a ser provisional.";i['notRepeatedMoves']="No hemos repetido movimientos, ¿por qué el juego sigue siendo tablas por repetición?";i['noUpperCaseDot']="No.";i['otherWaysToHelp']="otras formas de ayudar";i['ownerUniqueTrophies']=s("Ese trofeo es único en la historia de Lichess, nadie más que %1$s lo tendrá jamás.");i['pleaseReadFairPlayPage']=s("Para más información, lee nuestra %s");i['positions']="posiciones";i['preventLeavingGameWithoutResigning']="¿Qué se hace con los jugadores que abandonan las partidas sin rendirse?";i['provisionalRatingExplanation']="El signo de interrogación significa que la puntuación es provisional. Las razones incluyen:";i['ratingDeviationLowerThanXinChessYinVariants']=s("tener una desviación de puntuación menor que %1$s, en ajedrez estándar, y menor que %2$s en variantes,");i['ratingDeviationMorethanOneHundredTen']="Concretamente, significa que la desviación Glicko-2 es mayor que 110. La desviación es el nivel de confianza que tiene el sistema en la clasificación. Cuanto más baja sea la desviación, más estable será una puntuación.";i['ratingLeaderboards']="listado de líderes";i['ratingRefundExplanation']="Un minuto después de marcar a un jugador como tramposo, se toman sus últimas 40 partidas por puntos de los tres últimos días. Si tú fuiste su oponente en esas partidas, perdiste puntos en ellas (bien porque perdiste, bien porque empataste), y tu puntuación no era provisional, entonces se te devuelven puntos. Este reembolso está limitado por tu puntuación máxima y por cómo haya progresado tu puntuación después de aquella partida.\n(Por ejemplo, si tu puntuación ha subido mucho después de aquellas partidas, puede que no se te devuelvan los puntos o sólo se te devuelvan algunos). La devolución en ningún caso será de más de 150 puntos.";i['ratingSystemUsedByLichess']="Las puntuaciones se calculan utilizando el método de clasificación Glicko-2 desarrollado por Mark Glickman. Es un método de puntuación muy popular y que utilizan muchas organizaciones de ajedrez (la FIDE es un contraejemplo notable, ya que todavía utilizan el sistema de puntuación Elo).\n\nEn síntesis, las puntuaciones de Glicko usan \\\"intervalos de confianza\\\" al calcular y representar tu puntuación. Cuando comienzas a utilizar por primera vez el la página, tu puntuación es de 1500 ± 1000. El 1500 representa tu puntuación, y el 1000 representa el intervalo de confianza, que es una medida de la incertidumbre que el sistema tiene sobre tu puntuación real.\n\nBásicamente, el sistema tiene un 90% de seguridad de que tu puntuación está entre 500 y 2500. Es un intervalo muy amplio y poco preciso (hay mucha incertidumbre). Por eso, cuando un jugador acaba de empezar, su puntuación cambiará de forma muy brusca, incluso puede que varios cientos de puntos cada vez. Pero después de algunas partidas contra jugadores más veteranos, el intervalo de confianza se irá reduciendo (el sistema tendrá cada vez más clara cuál es su puntuación exacta), y la cantidad de puntos ganados o perdidos después de cada partida disminuirá.\n\nOtro punto que cabe señalar es que, si pasa un tiempo sin jugar, el intervalo de confianza volverá aumentar. Esto te permite ganar o perder puntos más rápidamente para igualar cualquier cambio en tu fuerza de juego durante ese tiempo que has pasado sin jugar.";i['repeatedPositionsThatMatters']=s("La triple repetición se da por repetición de %1$s, no de movimientos. La repetición no tiene porqué ocurrir consecutivamente.");i['secondRequirementToStopOldPlayersTrustingLeaderboards']="El segundo requisito es que los jugadores que ya no usan sus cuentas dejen de poblar el listado de líderes.";i['showYourTitle']=s("Si tienes un título obtenido en un torneo presencial, puedes solicitar que se muestre en tu cuenta completando el %1$s, incluyendo una imagen clara de un documento/tarjeta de identificación y una foto tuya portando dicho documento/tarjeta.\n\nLa verificación como jugador titulado en Lichess da acceso a jugar en los torneos para titulados.\n\nFinalmente, existe el título honorífico %2$s.");i['similarOpponents']="oponentes de fuerza similar";i['stopMyselfFromPlaying']="¿Parar de jugar?";i['superiorThanXsEqualYtimeControl']=s("≥ %1$s s = %2$s");i['threeFoldHasToBeClaimed']=s("Las tablas por triple repetición deben ser reclamadas por uno de los jugadores. Puedes hacerlo pulsando el botón que se muestra, u ofreciendo tablas antes de hacer el movimiento que cause la tercera repetición, no importará que tu oponente rechace la oferta de tablas, las tablas por triple repetición serán reclamadas de todas maneras. También puedes %1$s Lichess para que reclame automáticamente las repeticiones por ti. Además, la quinta repetición siempre termina la partida inmediatamente.");i['threefoldRepetition']="Triple repetición";i['threefoldRepetitionExplanation']=s("Si una posición ocurre tres veces, los jugadores pueden reclamar un empate por %1$s. Lichess implementa las reglas oficiales de la FIDE, de acuerdo con el Artículo 9.2 del %2$s.");i['threefoldRepetitionLowerCase']="triple repetición";i['titlesAvailableOnLichess']="¿Qué títulos hay en Lichess?";i['uniqueTrophies']="Trofeos únicos";i['usernamesCannotBeChanged']="No, los nombres de usuario no pueden cambiarse por razones técnicas y prácticas. Los nombres de usuario se materializan en muchos lugares: bases de datos, exportaciones, registros y en la mente de las personas. Puedes ajustar las mayúsculas/minúsculas solo una vez.";i['usernamesNotOffensive']=s("En general, los nombres de usuario no deben: ser ofensivos, suplantar a alguien o hacer publicidad. Puedes leer más sobre las %1$s.");i['verificationForm']="formulario de verificación";i['viewSiteInformationPopUp']="Ver notificación emergente de información del sitio";i['watchIMRosenCheckmate']=s("Ver al Maestro Internacional Eric Rosen dar mate a %s.");i['wayOfBerserkExplanation']=s("Para obtenerlo, hiimgosu se desafió a sí mismo a berserk y ganó todas las partidas de %s.");i['weCannotDoThatEvenIfItIsServerSideButThatsRare']="Desafortunadamente, no podemos devolver puntajes para las partidas perdidas debido a retraso o desconexión, independientemente de si el problema estaba de tu lado o del nuestro. Sin embargo, esto último es muy raro. También ten en cuenta que cuando Lichess se reinicia y pierdes el tiempo debido a eso, abortamos el juego para prevenir una pérdida injusta.";i['weRepeatedthreeTimesPosButNoDraw']="Hemos repetido una posición tres veces. ¿Por qué la partida no se declara tablas?";i['whatIsACPL']="¿Qué es la pérdida media en centipeones (PMC)?";i['whatIsProvisionalRating']="¿Por qué hay un signo de interrogación (?) junto a una puntuación?";i['whatUsernameCanIchoose']="¿Cuál puede ser mi nombre de usuario?";i['whatVariantsCanIplay']="¿Qué variantes puedo jugar en Lichess?";i['whenAmIEligibleRatinRefund']="¿En qué casos se me devuelven automáticamente los puntos perdidos contra un tramposo?";i['whichRatingSystemUsedByLichess']="¿Qué sistema de puntuación utiliza Lichess?";i['whyAreRatingHigher']="¿Por qué son más altas las puntuaciones en comparación con otras páginas y organizaciones como FIDE, USCF y el CIC?";i['whyAreRatingHigherExplanation']="Lo mejor es no considerar las puntuaciones como cifras absolutas ni compararlas con otras organizaciones. Diferentes organizaciones tienen distintos niveles de jugadores y sistemas de puntuación (Elo, Glicko, Glicko-2 o una versión modificada de los primeros). Estos factores pueden afectar drásticamente los números absolutos (puntuaciones).\n\nEs mejor pensar en las puntuaciones como figuras \\\"relativas\\\" (a diferencia de las \\\"figuras absolutas\\\"): dentro de un conjunto de jugadores, sus diferencias relativas en las puntuaciones te ayudarán a estimar quién gana/empata/pierde, y con qué frecuencia. Decir \\\"tengo puntuación X\\\" significa nada a menos que haya otros jugadores con los que comparar esa puntuación.";i['whyIsLichessCalledLichess']="¿Por qué Lichess se llama así?";i['whyIsLilaCalledLila']=s("Del mismo modo, el código fuente de Lichess, %1$s, significa li[chess en sca]la, dado que la mayor parte de Lichess está escrito en %2$s, un intuitivo lenguaje de programación.");i['whyLiveLightLibre']="En directo, porque los partidas se juegan y se ven en tiempo real 24/7; ligero y libre porque Lichess es de código abierto y sin la basura propietaria que abunda en otras páginas.";i['yesLichessInspiredOtherOpenSourceWebsites']=s("Sí. Lichess ha inspirado otras páginas de código abierto que utilizan nuestro %1$s, %2$s, o %3$s.");i['youCannotApply']="No se puede solicitar ser moderador. Si vemos a alguien que pensamos que sería un buen moderador, nos ponemos en contacto con él directamente.";i['youCanUseOpeningBookNoEngine']="En Lichess, la principal diferencia en las reglas del ajedrez por correspondencia es que se permite un libro de aperturas. El uso de motores sigue estando prohibido y dará lugar a que se etiquete al usuario como tramposo. Aunque la ICCF permite el uso del motor en partidas por correspondencia, Lichess no lo permite."})()