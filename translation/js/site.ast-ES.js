"use strict";(()=>{function o(t,r){return t[site.quantity(r)]||t.other||t.one||"no plural found"}function p(t){let r=(n,e)=>l(o(t,n),e).join("");return r.asArray=(n,e)=>l(o(t,n),e),r}function s(t){let r=n=>l(t,n).join("");return r.asArray=n=>l(t,n),r}function l(t,r){let n=t.split(/(%(?:d$)?s)/g);if(r.length){let e=n.indexOf("%s");if(e!==-1)n[e]=r[0];else for(let i=0;i<r.length;i++){let s=n.indexOf("%"+(i+1)+"$s");s!==-1&&(n[s]=r[i])}}return n}if(!window.i18n)window.i18n={};if(!window.i18n.site)window.i18n.site={};let i=window.i18n.site;i['aiNameLevelAiLevel']=s("%1$s nivel %2$s");i['analysis']="Tableru d\\'analís";i['asBlack']="como negres";i['asWhite']="como blanques";i['bestMoveArrow']="Flecha del meyor movimientu";i['black']="Negres";i['blackDidntMove']="Negres nun xugaron";i['blackIsVictorious']="Negres ganen";i['blackLeftTheGame']="Negres abandonaron";i['blackPlays']="Xuegan negres";i['blackResigned']="Negres rindiéronse";i['blunder']="Picia";i['byCPL']="Por PCP";i['calculatingMoves']="Calculando jugaes...";i['chat']="Chat";i['cheatDetected']="Trampes detectaes";i['checkmate']="Xaque mate";i['claimADraw']="Reclamar tables";i['cloudAnalysis']="Analís de la nube";i['computerAnalysis']="Analís d\\'ordenador";i['computerAnalysisAvailable']="Analís d\\'ordenador disponible";i['computerAnalysisDisabled']="Analís d\\'ordenador inutilizáu";i['cpus']="CPUes";i['createAGame']="Crear una partida";i['depthX']=s("Profundida %s");i['draw']="Tables";i['drawByMutualAgreement']="Tables per alcuerdu mutuu";i['enable']="Activar";i['engineFailed']="Error cargando módulo";i['evaluationGauge']="Mostrar evaluación";i['fiftyMovesWithoutProgress']="Cincuenta movimientos ensin progresu";i['flipBoard']="Xirar tableru";i['forceDraw']="Dexarlu en tables";i['forceResignation']="Reclamar la victoria";i['gameOver']="Xuegu termináu";i['goDeeper']="Ve mas profundo";i['inaccuracy']="Inexautitú";i['infiniteAnalysis']="Analís infinitu";i['itsYourTurn']="¡Ye la to vez!";i['joinTheGame']="Xunise a la partida";i['kingInTheCenter']="Rei nel centru";i['level']="Nivel";i['loadingEngine']="Modulo cargando...";i['memory']="Espaciu";i['mistake']="Fallu";i['moveTimes']="Tiempu ente movimientos";i['multipleLines']="Delles llínees";i['nbBlunders']=p({"one":"%s picia","other":"%s picies"});i['nbInaccuracies']=p({"one":"%s inexautitú","other":"%s inexautitúes"});i['nbMistakes']=p({"one":"%s fallu","other":"%s fallos"});i['nbPlayers']=p({"one":"%s xugador","other":"%s xugadores"});i['newOpponent']="Nuevu oponente";i['offerDraw']="Ufiertar tables";i['openStudy']="Estudiu abiertu";i['opponentLeftChoices']="El to oponente foise de la partida. Puedes reclamar la victoria, dexar la partida en tables o esperar.";i['opponentLeftCounter']=p({"one":"El to oponente foise de la partida. Puedes reclamar la victoria en %s segundu.","other":"El to oponente foise de la partida. Puedes reclamar la victoria en %s segundos."});i['playWithAFriend']="Xuega con un amigu";i['playWithTheMachine']="Xuega contra l\\'ordenador";i['raceFinished']="Carrera terminada";i['randomColor']="Llau aleatoriu";i['removesTheDepthLimit']="Quita la llende de fondura, y caltien el to ordenador caliente ;)";i['requestAComputerAnalysis']="Pidir un analís d\\'ordenador";i['resign']="Abandonar";i['showThreat']="Mostrar amenaza";i['stalemate']="Xuego afogáu";i['strength']="Dificultá";i['talkInChat']="¡Por favor, sé atentu nel chat!";i['theFirstPersonToComeOnThisUrlWillPlayWithYou']="La primer persona que busque esti enllaz va xugar contigo.";i['threeChecks']="Trés xaques";i['threefoldRepetition']="Repetición triple";i['toggleTheChat']="Alterna\\'l chat";i['toInviteSomeoneToPlayGiveThisUrl']="Para convidar a daquién a xugar, da-y esti enllaz";i['usingServerAnalysis']="Usando analís del servidor";i['variantEnding']="Final de variante";i['waiting']="Esperando";i['waitingForOpponent']="Esperando al oponente";i['white']="Blanques";i['whiteDidntMove']="Blanques nun xugaron";i['whiteIsVictorious']="Blanques ganen";i['whiteLeftTheGame']="Blanques abandonaron";i['whitePlays']="Xuegan blanques";i['whiteResigned']="Blanques rindiéronse";i['youPlayTheBlackPieces']="Xuegas con negres";i['youPlayTheWhitePieces']="Xuegas con blanques";i['yourOpponentWantsToPlayANewGameWithYou']="El to oponente quier una revancha";i['yourTurn']="La to vez"})()