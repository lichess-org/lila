"use strict";(()=>{function o(t,r){return t[site.quantity(r)]||t.other||t.one||"no plural found"}function p(t){let r=(n,e)=>l(o(t,n),e).join("");return r.asArray=(n,e)=>l(o(t,n),e),r}function s(t){let r=n=>l(t,n).join("");return r.asArray=n=>l(t,n),r}function l(t,r){let n=t.split(/(%(?:d$)?s)/g);if(r.length){let e=n.indexOf("%s");if(e!==-1)n[e]=r[0];else for(let i=0;i<r.length;i++){let s=n.indexOf("%"+(i+1)+"$s");s!==-1&&(n[s]=r[i])}}return n}if(!window.i18n)window.i18n={};if(!window.i18n.learn)window.i18n.learn={};let i=window.i18n.learn;i['advanced']="Abanzau";i['aPawnOnTheSecondRank']="Un peón en a segunda ringlera puede abanzar dos quadretz de vez!";i['attackTheOpponentsKing']="Ataca a lo rei enemigo";i['attackYourOpponentsKing']="Ataca lo rei rival de traza que no pueda desfender-se!";i['awesome']="Alucinant!";i['backToMenu']="Tornar a lo menú";i['bishopComplete']="Felicidatz! Sabes mover l\\'alfil.";i['bishopIntro']="Contino, imos a aprender como se mueve un alfil!";i['blackJustMovedThePawnByTwoSquares']="Las negras han moviu lo suyo peón dos quadretz! Captura-lo a lo paso.";i['boardSetup']="Disposición d\\'o escaquero";i['boardSetupComplete']="Felicidatz! Ya sabes cómo colocar las piezas en o escaquero.";i['boardSetupIntro']="Los dos exercitos s\\'enfrontinan, prestos pa la batalla.";i['byPlaying']="chugando!";i['capture']="Captura";i['captureAndDefendPieces']="Captura y desfiende piezas";i['captureComplete']="Felicidatz! Agora sabes luitar con as tuyas piezas!";i['captureIntro']="Identifica las piezas indefensas de l\\'oponent y captura-las!";i['captureThenPromote']="Captura y dimpués promociona lo peón!";i['castleKingSide']="Mueve lo tuyo rei dos quadretz pa fer un enroque curto.";i['castleKingSideMovePiecesFirst']="Fe un enroque curto. Fa falta aclarir lo camín en primeras.";i['castleQueenSide']="Mueve lo tuyo rei dos quadretz pa fer un enroque largo.";i['castleQueenSideMovePiecesFirst']="Fe un enroque largo. Fa falta aclarir lo camín en primeras.";i['castling']="Enroque";i['castlingComplete']="Norabuena! Conviene enrocar en quasi totas las partidas.";i['castlingIntro']="Mete a lo tuyo rei a salvo y desplega la tuya torre pa atacar!";i['checkInOne']="Escaque en una";i['checkInOneComplete']="Felicidatz! Has feito escaque a lo tuyo oponent, obligando-le a desfender lo suyo rei!";i['checkInOneGoal']="Ataca a lo rei enemigo en un movimiento!";i['checkInOneIntro']="Da escaque a lo rei rival Ye obligau a desfender-lo!";i['checkInTwo']="Escaque en dos";i['checkInTwoComplete']="Felicidatz! Has dau escaque a lo tuyo oponent, obligando-le a desfender lo suyo rei!";i['checkInTwoGoal']="Menaza a lo rei enemigo en dos movimientos!";i['checkInTwoIntro']="Troba la combinación de dos movimientos que da escaque a lo rei enemigo!";i['chessPieces']="Las piezas d\\'escaques";i['combat']="Combate";i['combatComplete']="Felicidatz! Agora sabes luitar con as tuyas piezas!";i['combatIntro']="Un buen guerrero sabe atacar y desfender!";i['defeatTheOpponentsKing']="Redota a lo rei enemigo";i['defendYourKing']="Desfiende a lo tuyo rei";i['dontLetThemTakeAnyUndefendedPiece']="No permitas que capturen garra pieza indefensa!";i['enPassant']="Captura a lo paso";i['enPassantComplete']="Norabuena! Ya sabes capturar a lo paso.";i['enPassantIntro']="Quan lo peón oponent mueve dos quadretz, puetz capturar-lo como si en hese moviu nomás una.";i['enPassantOnlyWorksImmediately']="La captura a lo paso nomás se permite immediatament dimpués que lo tuyo oponent mueva lo peón.";i['enPassantOnlyWorksOnFifthRank']="La captura a lo paso nomás se permite si lo tuyo peón ye en a quinta ringlera.";i['escape']="T\\'atacan! Libra-te de la menaza!";i['escapeOrBlock']="Escapa con o rei u bloca l\\'ataque!";i['escapeWithTheKing']="Escapa con o rei!";i['evaluatePieceStrength']="Sospesa la forza d\\'as tuyas piezas";i['excellent']="Excelent!";i['exerciseYourTacticalSkills']="Practica las tuyas habilidatz tacticas";i['findAWayToCastleKingSide']="Troba la forma de enrocar en curto!";i['findAWayToCastleQueenSide']="Troba la forma de enrocar en largo!";i['firstPlaceTheRooks']="En primeras, coloca las torres. Van en as cantonadas.";i['fundamentals']="Basico";i['getAFreeLichessAccount']="Fe-te una cuenta gratuita en Lichess";i['grabAllTheStars']="Captura totas las estrelas!";i['grabAllTheStarsNoNeedToPromote']="Captura totas las estrelas! No fa falta promocionar.";i['greatJob']="Buen treballo!";i['howTheGameStarts']="Cómo empecipiar";i['intermediate']="Intermedio";i['itMovesDiagonally']="Se mueve en diagonal";i['itMovesForwardOnly']="Nomás mueve enta adebant";i['itMovesInAnLShape']="Se mueve en forma de L";i['itMovesInStraightLines']="Se mueve en linia dreita";i['itNowPromotesToAStrongerPiece']="Agora promociona a una pieza mas fuerte.";i['keepYourPiecesSafe']="Mantiene las tuyas piezas seguras";i['kingComplete']="Ya puetz comandar lo comandante!";i['kingIntro']="Yes lo rei. Si cayes en batalla, se pierde la partida.";i['knightComplete']="Felicidatz! Dominas lo movimiento d\\'o caballo.";i['knightIntro']="A veyer cómo se te da este. Lo caballo ye... una pieza complicada.";i['knightsCanJumpOverObstacles']="Los caballos pueden blincar obstáculos! Escapa y captura las estrelas!";i['knightsHaveAFancyWay']="Los caballos tienen una forma curiosa de blincar!";i['lastOne']="Lo zaguer!";i['learnChess']="Aprende escaques";i['learnCommonChessPositions']="Aprende las posicions mas comuns";i['letsGo']="Prencipiemos!";i['mateInOne']="Mate en una";i['mateInOneComplete']="Felicidatz! Asinas ye como se ganan partidas!";i['mateInOneIntro']="Ganas quan lo tuyo oponent no puede desfender-se d\\'un escaque.";i['menu']="Menú";i['mostOfTheTimePromotingToAQueenIsBest']="La mayoría d\\'as vegadas, lo millor ye promocionar a dama pero i hai ocasions que un caballo puede estar util!";i['nailedIt']="L\\'has clavau!";i['next']="Siguient";i['nextX']=s("Siguient: %s");i['noEscape']="No puetz escapar, pero puetz desfender-te!";i['opponentsFromAroundTheWorld']="Oponents de tot lo mundo";i['outOfCheck']="Salir d\\'o escaque";i['outOfCheckComplete']="Felicidatz! No puetz permitir que capturen lo tuyo rei, tiene las tuyas defensas listas pa un escaque!";i['outOfCheckIntro']="Yes en escaque! Has de que escapar u blocar l\\'ataque.";i['outstanding']="Extraordinario!";i['pawnComplete']="Felicidatz! Los peons no tienen secretos pa tu.";i['pawnIntro']="Los peons son febles, pero amagan muito potencial.";i['pawnPromotion']="Promoción d\\'o peón";i['pawnsFormTheFrontLine']="Los peons forman la linia delantera. Mueve qualsequier pieza pa continar.";i['pawnsMoveForward']="Los peons abanzan en linia dreita, pero capturan en diagonal!";i['pawnsMoveOneSquareOnly']="Los peons nomás pueden mover un quadret, pero quan aconsiguen l\\'atro cabo d\\'o escaquero promocionan a una pieza mas fuerte!";i['perfect']="Perfecto!";i['pieceValue']="Valor d\\'as piezas";i['pieceValueComplete']="Felicidatz! Conoixes la valura d\\'as piezas!\nDama = 9\nTorre = 5\nAlfil = 3\nCaballo = 3\npeón = 1";i['pieceValueExchange']="Captura la pieza que tienga mas valura!\n No cambies\n una pieza que valga mas per una de menos valura.";i['pieceValueIntro']="Las piezas con mas mobilidat valen mas!\nDama = 9\nTorre = 5\nAlfil = 3\nCaballo = 3\npeón = 1\nLo rei ye valioso! Perder-lo significa perder la partida.";i['pieceValueLegal']="Captura la pieza \nque tienga mas valura!\nAsegura-te que lo movimiento ye legal.";i['placeTheBishops']="Coloca los alfils. Van a lo costau d\\'os caballos.";i['placeTheKing']="Coloca lo rei. A lo costau d\\'a dama.";i['placeTheQueen']="Coloca la dama. Va en a suya propia color.";i['play']="chugar!";i['playMachine']="Chugar contra la maquina";i['playPeople']="Chugar con atros";i['practice']="Practicar";i['progressX']=s("Progreso: %s");i['protection']="Protección";i['protectionComplete']="Felicidatz! Una pieza que no se pierde ye una pieza ganada!";i['protectionIntro']="Identifica las piezas que lo tuyo oponent menaza y desfiende-las!";i['puzzleFailed']="Solución incorrecta!";i['puzzles']="Problemas de tactica";i['queenCombinesRookAndBishop']="Dama = torre + alfil";i['queenComplete']="Felicidatz! La dama no tiene secretos pa tu.";i['queenIntro']="Y agora, la pieza mas poderosa d\\'os escaques; su machestat, la dama!";i['queenOverBishop']="Captura la pieza mas valurosa!\nDama > Alfil";i['register']="Rechistrar-se";i['resetMyProgress']="Reiniciar lo mío progreso";i['retry']="Reintentar";i['rightOn']="Exacto!";i['rookComplete']="Felicidatz! Has aconseguiu dominar la torre.";i['rookGoal']="Fe clic en a torre pa levar-la dica la estrela!";i['rookIntro']="La torre ye una pieza poderosa. Yes presto pa comandar-la?";i['selectThePieceYouWant']="Tría la pieza que quieras!";i['stageX']=s("Libel %s");i['stageXComplete']=s("Libel %s completo");i['stalemate']="Rei afogau";i['stalemateComplete']="Felicidatz! Millor afogau que recibir un escaque y mate!";i['stalemateGoal']="Las negras son afogadas sí:\n- No pueden fer garra movimiento legal, y\n- No son en escaque.";i['stalemateIntro']="Quan un chugador no ye en escaque y no tiene garra movimiento legal, ye rei afogau. La partida ye taulas (empaz). Dengún gana ni pierde.";i['takeAllThePawnsEnPassant']="Captura totz los peons a lo paso!";i['takeTheBlackPieces']="Captura las piezas negras!";i['takeTheBlackPiecesAndDontLoseYours']="Captura las piezas negras!\nY no pierdas las tuyas.";i['takeTheEnemyPieces']="Captura las piezas enemigas";i['takeThePieceWithTheHighestValue']="Captura la pieza mas valurosa!";i['testYourSkillsWithTheComputer']="Mete-te a preba contra l\\'ordinador";i['theBishop']="L\\'alfil";i['theFewerMoves']="Quantos menos movimientos faigas, mas puntos ganas!";i['theGameIsADraw']="La partida ye empaz";i['theKing']="Lo rei";i['theKingCannotEscapeButBlock']="Lo rei no puede escapar, pero puetz blocar l\\'ataque!";i['theKingIsSlow']="Lo rei ye lento.";i['theKnight']="Lo caballo";i['theKnightIsInTheWay']="Lo caballo se interpone! Mueve-lo, y dimpués fe un enroque curto.";i['theMostImportantPiece']="La pieza mas important";i['thenPlaceTheKnights']="Dimpués, coloca los caballos. Van a lo costau d\\'as torres.";i['thePawn']="Lo peón";i['theQueen']="La dama";i['theRook']="La torre";i['theSpecialKingMove']="Lo movimiento especial d\\'o rei";i['theSpecialPawnMove']="Lo movimiento especial d\\'o peón";i['thisIsTheInitialPosition']="Esta ye la posición inicial de qualsequier partida d\\'escaques. Mueve qualsequier pieza pa continar.";i['thisKnightIsCheckingThroughYourDefenses']="Este caballo ye dando-te escaque per alto d\\'os tuyos defensas!";i['twoMovesToGiveCheck']="Dar escaque en dos chugadas";i['useAllThePawns']="Utiliza totz los peons! No fa falta promocionar.";i['useTwoRooks']="Usa las dos torres pa rematar antes!";i['videos']="Videos";i['watchInstructiveChessVideos']="Mira videos educativos d\\'escaques";i['wayToGo']="Asinas se fa!";i['whatNext']="Y agora qué?";i['yesYesYes']="Sí, sí, sí!";i['youCanGetOutOfCheckByTaking']="Puetz salir d\\'o escaque capturando la pieza que ataca a lo rei.";i['youCannotCastleIfAttacked']="No puetz enrocar si lo rei ye atacau en o suyo camín. Bloca l\\'atacant y dimpués enroca!";i['youCannotCastleIfMoved']="No puetz enrocar si lo rei u la torre s\\'han moviu anteriorment.";i['youKnowHowToPlayChess']="Sabes chugar a los escaques, norabuena! Quiers aprender a chugar millor?";i['youNeedBothBishops']="Un alfil de quadretz blancos y un alfil de quadretz negros, los dos son necesarios!";i['youreGoodAtThis']="Se te da bien!";i['yourPawnReachedTheEndOfTheBoard']="Lo tuyo peón ha arribau a la fin d\\'o escaquero!";i['youWillLoseAllYourProgress']="Perderás totz los tuyos progresos!"})()