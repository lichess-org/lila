"use strict";(()=>{function o(t,r){return t[site.quantity(r)]||t.other||t.one||"no plural found"}function p(t){let r=(n,e)=>l(o(t,n),e).join("");return r.asArray=(n,e)=>l(o(t,n),e),r}function s(t){let r=n=>l(t,n).join("");return r.asArray=n=>l(t,n),r}function l(t,r){let n=t.split(/(%(?:d$)?s)/g);if(r.length){let e=n.indexOf("%s");if(e!==-1)n[e]=r[0];else for(let i=0;i<r.length;i++){let s=n.indexOf("%"+(i+1)+"$s");s!==-1&&(n[s]=r[i])}}return n}if(!window.i18n)window.i18n={};if(!window.i18n.learn)window.i18n.learn={};let i=window.i18n.learn;i['advanced']="Avançado";i['aPawnOnTheSecondRank']="Um peão na segunda fileira pode mover-se duas casas de uma vez!";i['attackTheOpponentsKing']="Ataque o rei do seu adversário";i['attackYourOpponentsKing']="Ataque o rei adversário de uma maneira que ele não possa se defender!";i['awesome']="Fantástico!";i['backToMenu']="Voltar ao menu";i['bishopComplete']="Parabéns! Você pode comandar um bispo.";i['bishopIntro']="A seguir, aprenderemos como o bispo se movimenta!";i['blackJustMovedThePawnByTwoSquares']="As pretas acabaram de mover o peão duas posições! Capture-o en passant.";i['boardSetup']="Posição inicial";i['boardSetupComplete']="Parabéns! Você sabe como posicionar as peças no tabuleiro de xadrez.";i['boardSetupIntro']="Os dois exércitos se enfrentam, prontos para a batalha.";i['byPlaying']="jogando!";i['capture']="Capturar";i['captureAndDefendPieces']="Capture e defenda peças";i['captureComplete']="Parabéns! Você já sabe como lutar com peças de xadrez!";i['captureIntro']="Identifique as peças desprotegidas do adversário, e capture-as!";i['captureThenPromote']="Capture, e então promova!";i['castleKingSide']="Mova o rei duas casas para fazer o roque curto!";i['castleKingSideMovePiecesFirst']="Faça um roque curto!\nMas antes você precisa retirar as peças do meio.";i['castleQueenSide']="Mova o rei duas casas\npara fazer o roque longo!";i['castleQueenSideMovePiecesFirst']="Faça um roque longo!\nMas antes você precisa retirar as peças do meio.";i['castling']="Roque";i['castlingComplete']="Parabéns! Você deve rocar em quase todas as partidas.";i['castlingIntro']="Traga o seu rei para a segurança, e prepare sua torre para o ataque!";i['checkInOne']="Xeque em 1";i['checkInOneComplete']="Parabéns! Você colocou o rei de seu oponente em xeque, obrigando-o a defendê-lo!";i['checkInOneGoal']="Ameace o rei de seu oponente em um movimento!";i['checkInOneIntro']="Para dar xeque, ataque o rei. Seu oponente deve defendê-lo!";i['checkInTwo']="Xeque em 2";i['checkInTwoComplete']="Parabéns! Você colocou em xeque o seu adversário, forçando-o a defender seu rei!";i['checkInTwoGoal']="Ameace o rei adversário em dois movimentos!";i['checkInTwoIntro']="Encontre a sequência correta de dois movimentos que coloque em xeque o rei adversário!";i['chessPieces']="As peças do xadrez";i['combat']="Combate";i['combatComplete']="Parabéns! Você sabe lutar com peças de xadrez!";i['combatIntro']="Um bom guerreiro sabe atacar e defender!";i['defeatTheOpponentsKing']="Derrote o rei adversário";i['defendYourKing']="Defenda-se de xeques";i['dontLetThemTakeAnyUndefendedPiece']="Não deixe que capturem qualquer peça indefesa!";i['enPassant']="En passant";i['enPassantComplete']="Parabéns! Você agora pode capturar en passant.";i['enPassantIntro']="Quando um peão do oponente mover-se duas posições, você pode capturá-lo como se ele tivesse movido apenas uma posição.";i['enPassantOnlyWorksImmediately']="En passant somente é possível imediatamente após o oponente mover o peão.";i['enPassantOnlyWorksOnFifthRank']="En passant somente é possível caso o seu peão esteja na 5ª linha.";i['escape']="Você está sob ataque!\nFuja da ameaça!";i['escapeOrBlock']="Escape com o rei\nou bloqueie o ataque!";i['escapeWithTheKing']="Escape com o rei!";i['evaluatePieceStrength']="Avalie o valor de cada peça";i['excellent']="Excelente!";i['exerciseYourTacticalSkills']="Exercite suas habilidades táticas";i['findAWayToCastleKingSide']="Encontre uma maneira de executar o roque menor (da ala do rei)!";i['findAWayToCastleQueenSide']="Encontre uma maneira de executar o roque maior (da ala da dama)!";i['firstPlaceTheRooks']="Primeiro posicione as torres!\nElas ficam nos cantos.";i['fundamentals']="Fundamentos";i['getAFreeLichessAccount']="Obtenha uma conta Lichess grátis";i['grabAllTheStars']="Pegue todas as estrelas!";i['grabAllTheStarsNoNeedToPromote']="Pegue todas as estrelas!\nNão é necessário promover.";i['greatJob']="Ótimo trabalho!";i['howTheGameStarts']="Como o jogo inicia";i['intermediate']="Intermediário";i['itMovesDiagonally']="Move-se em diagonais";i['itMovesForwardOnly']="Move-se apenas para frente";i['itMovesInAnLShape']="Move-se no formato de um L";i['itMovesInStraightLines']="Move-se em linha reta";i['itNowPromotesToAStrongerPiece']="Ele agora é promovido a uma peça mais forte.";i['keepYourPiecesSafe']="Mantenha suas peças em segurança";i['kingComplete']="Você agora pode comandar o comandante!";i['kingIntro']="Você é o rei. Se cair em batalha, perderá o jogo.";i['knightComplete']="Parabéns! Você acaba de tornar-se um mestre dos cavalos.";i['knightIntro']="Aqui está um desafio para você. O cavalo é... uma peça surpreendente.";i['knightsCanJumpOverObstacles']="Cavalos podem saltar sobre obstáculos!\nEscape e conquiste as estrelas!";i['knightsHaveAFancyWay']="Cavalos têm uma maneira peculiar de saltar por aí!";i['lastOne']="A última!";i['learnChess']="Aprenda xadrez";i['learnCommonChessPositions']="Aprenda posições comuns do xadrez";i['letsGo']="Vamos lá!";i['mateInOne']="Mate em 1";i['mateInOneComplete']="Parabéns! É assim que você vence jogos de xadrez!";i['mateInOneIntro']="Você vence quando seu oponente não pode se defender de um xeque.";i['menu']="Menu";i['mostOfTheTimePromotingToAQueenIsBest']="Na maioria das vezes o melhor é promovê-lo a uma dama.\nMas às vezes um cavalo pode ser útil!";i['nailedIt']="Acertou em cheio.";i['next']="Próximo";i['nextX']=s("Próximo: %s");i['noEscape']="Não há como fugir, \nmas você pode defender!";i['opponentsFromAroundTheWorld']="Oponentes de todo o mundo";i['outOfCheck']="Salve o rei";i['outOfCheckComplete']="Parabéns! Seu rei não deve ser tomado, certifique-se de que você pode defendê-lo de um xeque!";i['outOfCheckIntro']="Você está em xeque! Você deve escapar ou bloquear o ataque.";i['outstanding']="Excepcional!";i['pawnComplete']="Parabéns! Peões não têm segredos para você.";i['pawnIntro']="Peões são fracos, mas possuem grande potencial.";i['pawnPromotion']="Promoção do peão";i['pawnsFormTheFrontLine']="Peões formam a linha de frente.\nFaça qualquer movimento para continuar.";i['pawnsMoveForward']="Peões movem-se para a frente, mas capturam na diagonal!";i['pawnsMoveOneSquareOnly']="Peões andam apenas uma casa.\nMas quando alcançam o outro lado do tabuleiro, transformam-se em uma peça mais forte!";i['perfect']="Perfeito!";i['pieceValue']="Avaliando";i['pieceValueComplete']="Parabéns! Você conhece o valor material! \nDama = 9 \nTorre = 5 \nBispo = 3 \nCavalo = 3 \nPeão = 1";i['pieceValueExchange']="Tome a peça com o valor mais alto!\n Não troque\n uma peça de maior valor por outra de menor valor.";i['pieceValueIntro']="Peças com maior mobilidade possuem maior valor!\nDama = 9 Torre = 5\nBispo = 3 Cavalo = 3\nPeão = 1\nO rei é inestimável! Perdê-lo significa ser derrotado.";i['pieceValueLegal']="Tome a peça\ncom o valor mais alto!\nTenha certeza de que seu movimento é legal!";i['placeTheBishops']="Posicione os bispos!\nEles vão ao lado dos cavalos.";i['placeTheKing']="Posicione o rei!\nBem ao lado da sua dama.";i['placeTheQueen']="Posicione a dama!\nEla vai na casa de sua cor.";i['play']="jogar!";i['playMachine']="Computador";i['playPeople']="Jogue online";i['practice']="Praticar";i['progressX']=s("Progresso: %s");i['protection']="Defender";i['protectionComplete']="Parabéns! Cada peça que você não perde é uma peça que você ganha!";i['protectionIntro']="Identifique as peças que seu oponente está atacando e defenda-as!";i['puzzleFailed']="Quebra-cabeça falhou!";i['puzzles']="Quebra-cabeças";i['queenCombinesRookAndBishop']="Dama = torre + bispo";i['queenComplete']="Parabéns! A Dama não é segredo para você.";i['queenIntro']="Entra a mais poderosa das peças. Sua majestade, a dama!";i['queenOverBishop']="Capture a peça de maior valor!\nDama > Bispo";i['register']="Registrar";i['resetMyProgress']="Reiniciar meu progresso";i['retry']="Tentar de novo";i['rightOn']="Certo!";i['rookComplete']="Parabéns! Você dominou a torre com sucesso.";i['rookGoal']="Clique na torre para levá-la até a estrela!";i['rookIntro']="A torre é uma peça poderosa. Você está pronto para comandá-la?";i['selectThePieceYouWant']="Escolha a peça que desejar!";i['stageX']=s("Etapa %s");i['stageXComplete']=s("Etapa %s completa");i['stalemate']="Rei afogado";i['stalemateComplete']="Parabéns! Um empate é melhor do que uma derrota!";i['stalemateGoal']="Para afogar as pretas:\n- As pretas não podem mover-se para lugar algum\n- Não há xeque.";i['stalemateIntro']="Quando um jogador não está em xeque e não pode fazer nenhum movimento legal, há um afogamento. O jogo termina em empate: ninguém ganha, ninguém perde.";i['takeAllThePawnsEnPassant']="Capture todos os peões en passant!";i['takeTheBlackPieces']="Capture as peças pretas!";i['takeTheBlackPiecesAndDontLoseYours']="Capture as peças pretas!\nE não perca as suas.";i['takeTheEnemyPieces']="Capture as peças inimigas";i['takeThePieceWithTheHighestValue']="Capture a peça de maior valor!";i['testYourSkillsWithTheComputer']="Desafie o computador";i['theBishop']="O bispo";i['theFewerMoves']="Quanto menos movimentos você fizer, mais pontos ganhará!";i['theGameIsADraw']="O jogo terminou em empate";i['theKing']="O rei";i['theKingCannotEscapeButBlock']="O rei não pode escapar,\nmas você pode bloquear o ataque!";i['theKingIsSlow']="O rei é lento.";i['theKnight']="O cavalo";i['theKnightIsInTheWay']="O cavalo está no caminho!\nMova-o, e então faça um roque.";i['theMostImportantPiece']="A peça mais importante";i['thenPlaceTheKnights']="Agora posicione os cavalos!\nEles ficam ao lado das torres.";i['thePawn']="O peão";i['theQueen']="A dama";i['theRook']="A torre";i['theSpecialKingMove']="O movimento especial do rei";i['theSpecialPawnMove']="O movimento especial do peão";i['thisIsTheInitialPosition']="Esta é a posição inicial de qualquer jogo de xadrez! Faça qualquer movimento para continuar.";i['thisKnightIsCheckingThroughYourDefenses']="Este cavalo está dando xeque\natravés de suas defesas!";i['twoMovesToGiveCheck']="Dois movimentos para dar xeque";i['useAllThePawns']="Use todos os peões! \nNão é necessário promover.";i['useTwoRooks']="Use duas torres para acelerar as coisas!";i['videos']="Vídeos";i['watchInstructiveChessVideos']="Assista a vídeos de xadrez instrutivos";i['wayToGo']="Continue assim!";i['whatNext']="O que vem depois?";i['yesYesYes']="Sim, sim, sim!";i['youCanGetOutOfCheckByTaking']="Você pode sair de um xeque\ncapturando a peça atacante.";i['youCannotCastleIfAttacked']="Você não pode rocar se o rei está sob ameaça no caminho. Bloqueie o xeque, e então conclua o roque!";i['youCannotCastleIfMoved']="Você não pode rocar se o rei ou se a torre já se moveram.";i['youKnowHowToPlayChess']="Você sabe como jogar xadrez, parabéns! Você quer se tornar um jogador mais forte?";i['youNeedBothBishops']="Um bispo para as casas brancas,\num bispo para as casas pretas.\nVocê precisa de ambos!";i['youreGoodAtThis']="Você é bom nisso!";i['yourPawnReachedTheEndOfTheBoard']="Seu peão alcançou a última fileira do tabuleiro!";i['youWillLoseAllYourProgress']="Você vai perder todo o seu progresso!"})()