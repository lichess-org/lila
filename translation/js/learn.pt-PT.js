"use strict";(()=>{function o(t,r){return t[site.quantity(r)]||t.other||t.one||"no plural found"}function p(t){let r=(n,e)=>l(o(t,n),e).join("");return r.asArray=(n,e)=>l(o(t,n),e),r}function s(t){let r=n=>l(t,n).join("");return r.asArray=n=>l(t,n),r}function l(t,r){let n=t.split(/(%(?:d$)?s)/g);if(r.length){let e=n.indexOf("%s");if(e!==-1)n[e]=r[0];else for(let i=0;i<r.length;i++){let s=n.indexOf("%"+(i+1)+"$s");s!==-1&&(n[s]=r[i])}}return n}if(!window.i18n)window.i18n={};if(!window.i18n.learn)window.i18n.learn={};let i=window.i18n.learn;i['advanced']="Avançado";i['aPawnOnTheSecondRank']="Um peão na segunda fila pode mover-se duas casas duma vez!";i['attackTheOpponentsKing']="Ataca o rei do adversário";i['attackYourOpponentsKing']="Ataca o rei adversário\nde forma a que este não se possa defender!";i['awesome']="Fantástico!";i['backToMenu']="Voltar ao menu";i['bishopComplete']="Parabéns! Já sabes comandar o bispo.";i['bishopIntro']="A seguir vamos aprender como manobrar o bispo!";i['blackJustMovedThePawnByTwoSquares']="As pretas acabaram de\navançar o peão duas casas!\nCaptura-o en passant.";i['boardSetup']="Organização do tabuleiro";i['boardSetupComplete']="Parabéns! Agora sabes como organizar o tabuleiro de xadrez.";i['boardSetupIntro']="Os dois exércitos cara a cara, preparados para a batalha.";i['byPlaying']="a jogar!";i['capture']="Captura";i['captureAndDefendPieces']="Captura e defende peças";i['captureComplete']="Parabéns! Já sabes como lutar com as peças de xadrez!";i['captureIntro']="Identifica as peças não defendidas do adversário, captura-las!";i['captureThenPromote']="Come, depois promove!";i['castleKingSide']="Move o teu rei duas casas\npara rocares para o lado do rei!";i['castleKingSideMovePiecesFirst']="Roca para o lado do rei!\nPrecisas de afastar as tuas peças primeiro.";i['castleQueenSide']="Move o teu rei duas casas\npara rocares no lado da dama!";i['castleQueenSideMovePiecesFirst']="Roca para o lado da dama!\nPrecisas de afastar as tuas peças primeiro.";i['castling']="Roque";i['castlingComplete']="Parabéns! Deves fazer o roque durante um jogo, quase sempre.";i['castlingIntro']="Trás o teu rei para a segurança e leva a tua torre para o ataque!";i['checkInOne']="Xeque num lance";i['checkInOneComplete']="Parabéns! Fizeste xeque ao adversário, forçando-os a defender o seu rei!";i['checkInOneGoal']="Aponta ao rei adversário\nem apenas um movimento!";i['checkInOneIntro']="Para fazeres xeque ao adversário, ataca o seu rei. Eles têm de defendê-lo!";i['checkInTwo']="Xeque em dois lances";i['checkInTwoComplete']="Parabéns! Fizeste xeque ao adversário, forçando-o a defender o seu rei!";i['checkInTwoGoal']="Ameaça o rei adversário\nem dois movimentos!";i['checkInTwoIntro']="Encontra a combinação correta de dois movimentos que faz xeque ao rei adversário!";i['chessPieces']="Peças de xadrez";i['combat']="Combate";i['combatComplete']="Parabéns! Sabes como lutar com as peças de xadrez!";i['combatIntro']="Um bom guerreiro sabe tanto atacar como defender!";i['defeatTheOpponentsKing']="Derrota o rei do adversário";i['defendYourKing']="Defende o teu rei";i['dontLetThemTakeAnyUndefendedPiece']="Não os deixes capturar\nas peças não defendidas!";i['enPassant']="En passant";i['enPassantComplete']="Parabéns! Agora sabes capturar en passant.";i['enPassantIntro']="Quando um peão do adversário avança duas casas, podes capturá-lo com um peão como se ele só tivesse avançado uma casa.";i['enPassantOnlyWorksImmediately']="O en passant só funciona\nimediatamente depois do adversário\nmover o peão.";i['enPassantOnlyWorksOnFifthRank']="O en passant só funciona\nse o teu peão estiver na quinta linha.";i['escape']="Estás a ser atacado!\nEscapa da ameaça!";i['escapeOrBlock']="Escapa com o rei\nou bloqueia o ataque!";i['escapeWithTheKing']="Escapa com o rei!";i['evaluatePieceStrength']="Avalia a força das peças";i['excellent']="Excelente!";i['exerciseYourTacticalSkills']="Pratica as tuas habilidades táticas";i['findAWayToCastleKingSide']="Encontra uma maneira de\nfazer o roque do lado do rei!";i['findAWayToCastleQueenSide']="Encontra a maneira de\nfazer o roque grande!";i['firstPlaceTheRooks']="Primeiro coloca as torres!\nElas ficam nos cantos.";i['fundamentals']="Fundamentos";i['getAFreeLichessAccount']="Obtém um conta gratuita no Lichess";i['grabAllTheStars']="Apanha todas as estrelas!";i['grabAllTheStarsNoNeedToPromote']="Apanha todas as estrelas!\nNão é necessário promover.";i['greatJob']="Bom trabalho!";i['howTheGameStarts']="Como começa o jogo";i['intermediate']="Intermédio";i['itMovesDiagonally']="Move-se diagonalmente";i['itMovesForwardOnly']="Move-se só para a frente";i['itMovesInAnLShape']="Move-se em forma de L";i['itMovesInStraightLines']="Move-se em linhas retas";i['itNowPromotesToAStrongerPiece']="Agora promove-se a uma peça mais forte.";i['keepYourPiecesSafe']="Mantém as tuas peças seguras";i['kingComplete']="Agora tu podes comandar o comandante!";i['kingIntro']="Tu és o rei. Se cais em batalha, o jogo está perdido.";i['knightComplete']="Parabéns! Já dominas o cavalo.";i['knightIntro']="Aqui está um desafio para ti. O cavalo é... uma peça manhosa.";i['knightsCanJumpOverObstacles']="Os cavalos podem saltar obstáculos.\nEscapa e vence as estrelas!";i['knightsHaveAFancyWay']="Os cavalos têm uma maneira divertida\nde saltar por aí!";i['lastOne']="Último!";i['learnChess']="Aprende xadrez";i['learnCommonChessPositions']="Aprende posições comuns no xadrez";i['letsGo']="Vamos!";i['mateInOne']="Xeque-mate num lance";i['mateInOneComplete']="Parabéns! É assim que se ganham os jogos de xadrez!";i['mateInOneIntro']="Tu ganhas quando o teu adversário não se pode defender de um xeque.";i['menu']="Menu";i['mostOfTheTimePromotingToAQueenIsBest']="Na maioria das vezes promover a dama é o melhor.\nMas às vezes um cavalo é mais útil!";i['nailedIt']="Acertaste em cheio.";i['next']="Seguinte";i['nextX']=s("Próximo: %s");i['noEscape']="Não há escapatória,\nmas podes defender-te!";i['opponentsFromAroundTheWorld']="Adversários de todo o mundo";i['outOfCheck']="Sair do xeque";i['outOfCheckComplete']="Parabéns! O teu rei nunca pode ser capturado, certifica-te que podes sempre defender contra um xeque!";i['outOfCheckIntro']="Estás em xeque! Tens de escapar ou bloquear o ataque.";i['outstanding']="Excecional!";i['pawnComplete']="Parabéns! Os peões já não têm segredos para ti.";i['pawnIntro']="Os peões são fracos, mas têm muito potencial.";i['pawnPromotion']="Promoção do peão";i['pawnsFormTheFrontLine']="Os peões formam a linha da frente.\nFaz qualquer movimentos para continuar.";i['pawnsMoveForward']="Os peões mexem-se para a frente,\nmas comem na diagonal!";i['pawnsMoveOneSquareOnly']="Os peões andam só uma casa de cada vez.\nMas quando chegam ao final do tabuleiro, transformam-se numa peça mais forte!";i['perfect']="Perfeito!";i['pieceValue']="Valor das peças";i['pieceValueComplete']="Parabéns! Sabes o valor do material!\nDama = 9\nTorre = 5\nBispo = 3\nCavalo = 3\nPeão = 1";i['pieceValueExchange']="Tome a peça de maior valor! Não troque uma peça de valor mais alto por uma de menor valor.";i['pieceValueIntro']="Peças com alta mobilidade têm maior valor!\nDama = 9\nTorre = 5\nBispo = 3\nCavalo = 3\nPeão = 1\nO rei não tem preço! Perdê-lo significa perder o jogo.";i['pieceValueLegal']="Captura a peça\nCom o valor mais alto!\nConfirma que a tua jogada é legal!";i['placeTheBishops']="Coloca os bispos!\nEles ficam ao lado dos cavalos.";i['placeTheKing']="Coloca o rei!\nAo lado da dama.";i['placeTheQueen']="Coloca a dama!\nEla fica na casa da sua cor.";i['play']="joga!";i['playMachine']="Joga com o computador";i['playPeople']="Joga com pessoas";i['practice']="Pratica";i['progressX']=s("Progresso: %s");i['protection']="Proteção";i['protectionComplete']="Parabéns! Uma peça que não perdes é uma peça que ganhas!";i['protectionIntro']="Identifica as peças que o teu adversário está a atacar, e defende-as!";i['puzzleFailed']="Exercício falhado!";i['puzzles']="Problemas";i['queenCombinesRookAndBishop']="Dama = torre + bispo";i['queenComplete']="Parabéns! As damas não têm segredos para ti.";i['queenIntro']="Entra a peça mais poderosa do xadrez. Sua majestade, a dama!";i['queenOverBishop']="Captura a peça\nde maior valor!\nDama > bispo";i['register']="Regista-te";i['resetMyProgress']="Reiniciar o meu progresso";i['retry']="Tentar novamente";i['rightOn']="Assim mesmo!";i['rookComplete']="Parabéns! Agora dominas a torre.";i['rookGoal']="Clica na torre\npara levá-la até à estrela!";i['rookIntro']="A torre é uma peça poderosa. Estás preparado(a) para comandá-la?";i['selectThePieceYouWant']="Escolhe a peça que queres!";i['stageX']=s("Fase %s");i['stageXComplete']=s("Fase %s completa");i['stalemate']="Rei afogado";i['stalemateComplete']="Parabéns! É melhor teres o teu rei afogado do que levares xeque-mate!";i['stalemateGoal']="Para afogar o rei das pretas:\n- as pretas não se podem mexer\n- não há xeque.";i['stalemateIntro']="Quando um jogador não está em xeque e não pode fazer um movimento legal, então diz-se que o rei esta afogado. O jogo é empatado: ninguém ganha, ninguém perde.";i['takeAllThePawnsEnPassant']="Captura todos os peões en passant!";i['takeTheBlackPieces']="Captura as peças pretas!";i['takeTheBlackPiecesAndDontLoseYours']="Captura as peças pretas!\nE não percas as tuas.";i['takeTheEnemyPieces']="Captura as peças inimigas";i['takeThePieceWithTheHighestValue']="Captura a peça\nde maior valor!";i['testYourSkillsWithTheComputer']="Testa a tuas habilidades com o computador";i['theBishop']="O bispo";i['theFewerMoves']="Quantos menos movimentos fizeres,\nmais pontos ganhas!";i['theGameIsADraw']="O jogo é um empate";i['theKing']="O rei";i['theKingCannotEscapeButBlock']="O rei não pode escapar,\nmas podes bloquear o ataque!";i['theKingIsSlow']="O rei é lento.";i['theKnight']="O cavalo";i['theKnightIsInTheWay']="O cavalo está no caminho!\nMove-o, depois roca para o lado do rei.";i['theMostImportantPiece']="A peça mais importante";i['thenPlaceTheKnights']="Depois coloca os cavalos!\nEles ficam ao lado das torres.";i['thePawn']="O peão";i['theQueen']="A dama";i['theRook']="A torre";i['theSpecialKingMove']="O movimento especial do rei";i['theSpecialPawnMove']="O movimento especial do peão";i['thisIsTheInitialPosition']="Esta é a posição inicial\nde todos os jogos de xadrez!\nFaz qualquer movimento para continuar.";i['thisKnightIsCheckingThroughYourDefenses']="Este cavalo está a fazer-te xeque\natravés das tuas defesas!";i['twoMovesToGiveCheck']="Dois movimentos para fazer xeque";i['useAllThePawns']="Usa todos os peões!\nNão é necessário promover.";i['useTwoRooks']="Usa as duas torres\npara acelerar o processo!";i['videos']="Vídeos";i['watchInstructiveChessVideos']="Vê vídeos instrutivos de xadrez";i['wayToGo']="Boa!";i['whatNext']="O que se segue?";i['yesYesYes']="Sim, sim, sim!";i['youCanGetOutOfCheckByTaking']="Podes sair do xeque\ncapturando a peça atacante.";i['youCannotCastleIfAttacked']="Não fazer o roque se\no teu rei for atacado no caminho.\nBloqueia o xeque e depois roca!";i['youCannotCastleIfMoved']="Não podes fazer o roque se\no teu rei ou a torre correspondente\njá executaram algum movimento.";i['youKnowHowToPlayChess']="Tu sabes como jogar xadrez, parabéns! Queres tornar-te um jogador mais forte?";i['youNeedBothBishops']="Um bispo de casas brancas,\num bispo de casas negras.\nTu precisas de ambos!";i['youreGoodAtThis']="És bom nisto!";i['yourPawnReachedTheEndOfTheBoard']="O teu peão alcançou o final do tabuleiro!";i['youWillLoseAllYourProgress']="Vais perder todo o teu progresso!"})()