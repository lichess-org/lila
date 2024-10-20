// @ts-nocheck

function p(t) {
  let n = (e, ...i) => l(o(t, e), e, ...i).join('');
  return (n.asArray = (e, ...i) => l(o(t, e), ...i)), n;
}
function s(t) {
  let n = (...e) => l(t, ...e).join('');
  return (n.asArray = (...e) => l(t, ...e)), n;
}
function o(t, n) {
  return t[site.quantity(n)] || t.other || t.one || '';
}
function l(t, ...n) {
  let e = t.split(/(%(?:\d\$)?s)/);
  if (n.length) {
    let i = e.indexOf('%s');
    if (i !== -1) e[i] = n[0];
    else
      for (let r = 0; r < n.length; r++) {
        let f = e.indexOf('%' + (r + 1) + '$s');
        f !== -1 && (e[f] = n[r]);
      }
  }
  return e;
}
if (!window.i18n.learn) (window.i18n as any).learn = {};
let i = window.i18n.learn;
i['advanced'] = 'Advanced';
i['aPawnOnTheSecondRank'] = 'A pawn on the second rank can move 2 squares at once!';
i['attackTheOpponentsKing'] = "Attack the opponent's king";
i['attackYourOpponentsKing'] = "Attack your opponent's king\nin a way that cannot be defended!";
i['awesome'] = 'Awesome!';
i['backToMenu'] = 'Back to menu';
i['bishopComplete'] = 'Congratulations! You can command a bishop.';
i['bishopIntro'] = 'Next we will learn how to manoeuvre a bishop!';
i['blackJustMovedThePawnByTwoSquares'] = 'Black just moved the pawn\nby two squares!\nTake it en passant.';
i['boardSetup'] = 'Board setup';
i['boardSetupComplete'] = 'Congratulations! You know how to set up the chess board.';
i['boardSetupIntro'] = 'The two armies face each other, ready for the battle.';
i['byPlaying'] = 'by playing!';
i['capture'] = 'Capture';
i['captureAndDefendPieces'] = 'Capture and defend pieces';
i['captureComplete'] = 'Congratulations! You know how to fight with chess pieces!';
i['captureIntro'] = "Identify the opponent's undefended pieces, and capture them!";
i['captureThenPromote'] = 'Capture, then promote!';
i['castleKingSide'] = 'Move your king two squares\nto castle king-side!';
i['castleKingSideMovePiecesFirst'] = 'Castle king-side!\nYou need to move out pieces first.';
i['castleQueenSide'] = 'Move your king two squares\nto castle queen-side!';
i['castleQueenSideMovePiecesFirst'] = 'Castle queen-side!\nYou need to move out pieces first.';
i['castling'] = 'Castling';
i['castlingComplete'] = 'Congratulations! You should almost always castle in a game.';
i['castlingIntro'] = 'Bring your king to safety, and deploy your rook for attack!';
i['checkInOne'] = 'Check in one';
i['checkInOneComplete'] = 'Congratulations! You checked your opponent, forcing them to defend their king!';
i['checkInOneGoal'] = "Aim at the opponent's king\nin one move!";
i['checkInOneIntro'] = 'To check your opponent, attack their king. They must defend it!';
i['checkInTwo'] = 'Check in two';
i['checkInTwoComplete'] = 'Congratulations! You checked your opponent, forcing them to defend their king!';
i['checkInTwoGoal'] = "Threaten the opponent's king\nin two moves!";
i['checkInTwoIntro'] = "Find the right combination of two moves that checks the opponent's king!";
i['chessPieces'] = 'Chess pieces';
i['combat'] = 'Combat';
i['combatComplete'] = 'Congratulations! You know how to fight with chess pieces!';
i['combatIntro'] = 'A good warrior knows both attack and defence!';
i['defeatTheOpponentsKing'] = "Defeat the opponent's king";
i['defendYourKing'] = 'Defend your king';
i['dontLetThemTakeAnyUndefendedPiece'] = "Don't let them take\nany undefended piece!";
i['enPassant'] = 'En passant';
i['enPassantComplete'] = 'Congratulations! You can now take en passant.';
i['enPassantIntro'] =
  'When the opponent pawn moved by two squares, you can take it like if it moved by one square.';
i['enPassantOnlyWorksImmediately'] = 'En passant only works\nimmediately after the opponent\nmoved the pawn.';
i['enPassantOnlyWorksOnFifthRank'] = 'En passant only works\nif your pawn is on the 5th rank.';
i['escape'] = "You're under attack!\nEscape the threat!";
i['escapeOrBlock'] = 'Escape with the king\nor block the attack!';
i['escapeWithTheKing'] = 'Escape with the king!';
i['evaluatePieceStrength'] = 'Evaluate piece strength';
i['excellent'] = 'Excellent!';
i['exerciseYourTacticalSkills'] = 'Exercise your tactical skills';
i['findAWayToCastleKingSide'] = 'Find a way to\ncastle king-side!';
i['findAWayToCastleQueenSide'] = 'Find a way to\ncastle queen-side!';
i['firstPlaceTheRooks'] = 'First place the rooks!\nThey go in the corners.';
i['fundamentals'] = 'Fundamentals';
i['getAFreeLichessAccount'] = 'Get a free Lichess account';
i['grabAllTheStars'] = 'Grab all the stars!';
i['grabAllTheStarsNoNeedToPromote'] = 'Grab all the stars!\nNo need to promote.';
i['greatJob'] = 'Great job!';
i['howTheGameStarts'] = 'How the game starts';
i['intermediate'] = 'Intermediate';
i['itMovesDiagonally'] = 'It moves diagonally';
i['itMovesForwardOnly'] = 'It moves forward only';
i['itMovesInAnLShape'] = 'It moves in an L shape';
i['itMovesInStraightLines'] = 'It moves in straight lines';
i['itNowPromotesToAStrongerPiece'] = 'It now promotes to a stronger piece.';
i['keepYourPiecesSafe'] = 'Keep your pieces safe';
i['kingComplete'] = 'You can now command the commander!';
i['kingIntro'] = 'You are the king. If you fall in battle, the game is lost.';
i['knightComplete'] = 'Congratulations! You have mastered the knight.';
i['knightIntro'] = "Here's a challenge for you. The knight is... a tricky piece.";
i['knightsCanJumpOverObstacles'] = 'Knights can jump over obstacles!\nEscape and vanquish the stars!';
i['knightsHaveAFancyWay'] = 'Knights have a fancy way\nof jumping around!';
i['lastOne'] = 'Last one!';
i['learnChess'] = 'Learn chess';
i['learnCommonChessPositions'] = 'Learn common chess positions';
i['letsGo'] = "Let's go!";
i['mateInOne'] = 'Mate in one';
i['mateInOneComplete'] = 'Congratulations! That is how you win chess games!';
i['mateInOneIntro'] = 'You win when your opponent cannot defend against a check.';
i['menu'] = 'Menu';
i['mostOfTheTimePromotingToAQueenIsBest'] =
  'Most of the time promoting to a queen is the best.\nBut sometimes a knight can come in handy!';
i['nailedIt'] = 'Nailed it.';
i['next'] = 'Next';
i['nextX'] = s('Next: %s');
i['noEscape'] = 'There is no escape,\nbut you can defend!';
i['opponentsFromAroundTheWorld'] = 'Opponents from around the world';
i['outOfCheck'] = 'Out of check';
i['outOfCheckComplete'] =
  'Congratulations! Your king can never be taken, make sure you can defend against a check!';
i['outOfCheckIntro'] = 'You are in check! You must escape or block the attack.';
i['outstanding'] = 'Outstanding!';
i['pawnComplete'] = 'Congratulations! Pawns have no secrets for you.';
i['pawnIntro'] = 'Pawns are weak, but they pack a lot of potential.';
i['pawnPromotion'] = 'Pawn promotion';
i['pawnsFormTheFrontLine'] = 'Pawns form the front line.\nMake any move to continue.';
i['pawnsMoveForward'] = 'Pawns move forward,\nbut capture diagonally!';
i['pawnsMoveOneSquareOnly'] =
  'Pawns move one square only.\nBut when they reach the other side of the board, they become a stronger piece!';
i['perfect'] = 'Perfect!';
i['pieceValue'] = 'Piece value';
i['pieceValueComplete'] =
  'Congratulations! You know the value of material!\nQueen = 9\nRook = 5\nBishop = 3\nKnight = 3\nPawn = 1';
i['pieceValueExchange'] =
  'Take the piece with the highest value!\n Do not exchange\n a higher valued piece for a less valuable one.';
i['pieceValueIntro'] =
  'Pieces with high mobility have a higher value!\nQueen = 9\nRook = 5\nBishop = 3\nKnight = 3\nPawn = 1\nThe king is priceless! Losing it means losing the game.';
i['pieceValueLegal'] = 'Take the piece\nwith the highest value!\nMake sure your move is legal!';
i['placeTheBishops'] = 'Place the bishops!\nThey go next to the knights.';
i['placeTheKing'] = 'Place the king!\nRight next to his queen.';
i['placeTheQueen'] = 'Place the queen!\nShe goes on her own colour.';
i['play'] = 'play!';
i['playMachine'] = 'Play machine';
i['playPeople'] = 'Play people';
i['practice'] = 'Practise';
i['progressX'] = s('Progress: %s');
i['protection'] = 'Protection';
i['protectionComplete'] = "Congratulations! A piece you don't lose is a piece you win!";
i['protectionIntro'] = 'Identify the pieces your opponent attacks, and defend them!';
i['puzzleFailed'] = 'Puzzle failed!';
i['puzzles'] = 'Puzzles';
i['queenCombinesRookAndBishop'] = 'Queen = rook + bishop';
i['queenComplete'] = 'Congratulations! Queens have no secrets for you.';
i['queenIntro'] = 'The most powerful chess piece enters. Her majesty the queen!';
i['queenOverBishop'] = 'Take the piece\nwith the highest value!\nQueen > Bishop';
i['register'] = 'Register';
i['resetMyProgress'] = 'Reset my progress';
i['retry'] = 'Retry';
i['rightOn'] = 'Right on!';
i['rookComplete'] = 'Congratulations! You have successfully mastered the rook.';
i['rookGoal'] = 'Click on the rook\nto bring it to the star!';
i['rookIntro'] = 'The rook is a powerful piece. Are you ready to command it?';
i['selectThePieceYouWant'] = 'Select the piece you want!';
i['stageX'] = s('Stage %s');
i['stageXComplete'] = s('Stage %s complete');
i['stalemate'] = 'Stalemate';
i['stalemateComplete'] = 'Congratulations! Better be stalemated than checkmated!';
i['stalemateGoal'] = 'To stalemate black:\n- Black cannot move anywhere\n- There is no check.';
i['stalemateIntro'] =
  "When a player is not in check and does not have a legal move, it's a stalemate. The game is drawn: no one wins, no one loses.";
i['takeAllThePawnsEnPassant'] = 'Take all the pawns en passant!';
i['takeTheBlackPieces'] = 'Take the black pieces!';
i['takeTheBlackPiecesAndDontLoseYours'] = "Take the black pieces!\nAnd don't lose yours.";
i['takeTheEnemyPieces'] = 'Take the enemy pieces';
i['takeThePieceWithTheHighestValue'] = 'Take the piece\nwith the highest value!';
i['testYourSkillsWithTheComputer'] = 'Test your skills with the computer';
i['theBishop'] = 'The bishop';
i['theFewerMoves'] = 'The fewer moves you make,\nthe more points you win!';
i['theGameIsADraw'] = 'The game is a draw';
i['theKing'] = 'The king';
i['theKingCannotEscapeButBlock'] = 'The king cannot escape,\nbut you can block the attack!';
i['theKingIsSlow'] = 'The king is slow.';
i['theKnight'] = 'The knight';
i['theKnightIsInTheWay'] = 'The knight is in the way!\nMove it, then castle king-side.';
i['theMostImportantPiece'] = 'The most important piece';
i['thenPlaceTheKnights'] = 'Then place the knights!\nThey go next to the rooks.';
i['thePawn'] = 'The pawn';
i['theQueen'] = 'The queen';
i['theRook'] = 'The rook';
i['theSpecialKingMove'] = 'The special king move';
i['theSpecialPawnMove'] = 'The special pawn move';
i['thisIsTheInitialPosition'] =
  'This is the initial position\nof every game of chess!\nMake any move to continue.';
i['thisKnightIsCheckingThroughYourDefenses'] = 'This knight is checking\nthrough your defences!';
i['twoMovesToGiveCheck'] = 'Two moves to give a check';
i['useAllThePawns'] = 'Use all the pawns!\nNo need to promote.';
i['useTwoRooks'] = 'Use two rooks\nto speed things up!';
i['videos'] = 'Videos';
i['watchInstructiveChessVideos'] = 'Watch instructive chess videos';
i['wayToGo'] = 'Way to go!';
i['whatNext'] = 'What next?';
i['yesYesYes'] = 'Yes, yes, yes!';
i['youCanGetOutOfCheckByTaking'] = 'You can get out of check\nby taking the attacking piece.';
i['youCannotCastleIfAttacked'] =
  'You cannot castle if\nthe king is attacked on the way.\nBlock the check then castle!';
i['youCannotCastleIfMoved'] =
  'You cannot castle if\nthe king has already moved\nor the rook has already moved.';
i['youKnowHowToPlayChess'] =
  'You know how to play chess, congratulations! Do you want to become a stronger player?';
i['youNeedBothBishops'] = 'One light-squared bishop,\none dark-squared bishop.\nYou need both!';
i['youreGoodAtThis'] = "You're good at this!";
i['yourPawnReachedTheEndOfTheBoard'] = 'Your pawn reached the end of the board!';
i['youWillLoseAllYourProgress'] = 'You will lose all your progress!';

export const i18nHotfix = i;
