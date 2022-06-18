import { and, checkmate, not, or, pieceOn } from '../assert';
import { IncompleteLevel, IncompleteStage } from '../interfaces';
import { createLevel } from '../level';
import { circle, initial } from '../shapes';
import { toPiece } from '../util';

const levels: IncompleteLevel[] = [
  {
    goal: 'toWinInShogi',
    sfen: '6k2/9/6P2/9/9/9/9/9/9 b G2r2b3g4s4n4l17p 1',
    nbMoves: 1,
    success: checkmate,
    failure: not(checkmate),
    drawShapes: initial([circle(toPiece('G'))]),
    showFailureMove: 'random',
  },
  {
    goal: 'dropsCommonDeliverMate',
    sfen: '7k1/9/7S1/9/9/9/9/9/9 b S2r2b4g2s4n4l18p 1',
    nbMoves: 1,
    success: checkmate,
    failure: not(checkmate),
    showFailureMove: 'random',
  },
  {
    goal: 'attackYourOpponentsKing',
    sfen: '7nk/7bl/9/9/6N2/9/9/9/9 b 2rb4g4s2n3l18p 1',
    nbMoves: 1,
    success: checkmate,
    failure: not(checkmate),
    showFailureMove: 'random',
  },
  {
    goal: 'attackYourOpponentsKing',
    sfen: '4R3G/7k1/6ppp/9/9/9/9/9/9 b r2b3g4s4n4l15p 1',
    nbMoves: 1,
    success: checkmate,
    failure: not(checkmate),
    showFailureMove: 'random',
  },
  {
    goal: 'attackYourOpponentsKing',
    sfen: '7kl/7s1/6PSp/9/9/9/9/9/9 b 2r2b4g2s4n3l16p 1',
    nbMoves: 1,
    success: checkmate,
    failure: not(checkmate),
    showFailureMove: 'random',
  },
  {
    goal: 'attackYourOpponentsKing',
    sfen: '9/5n2+B/5pk2/9/7+R1/9/9/9/9 b rb4g4s3n4l17p 1',
    nbMoves: 1,
    success: checkmate,
    failure: not(checkmate),
    showFailureMove: 'random',
  },
  {
    goal: 'attackYourOpponentsKing',
    sfen: '5k3/6s2/5NB2/9/9/9/9/9/9 b 2rb4g3s3n4l18p 2',
    nbMoves: 1,
    success: checkmate,
    failure: not(checkmate),
    showFailureMove: 'random',
  },
  {
    goal: 'attackYourOpponentsKing',
    sfen: '4R3l/6g1k/5gsp1/6p1P/9/9/9/9/8L b r2b2g3s4n2l15p 1',
    nbMoves: 1,
    success: checkmate,
    failure: not(checkmate),
    showFailureMove: 'random',
  },
  {
    goal: 'chooseYourPieceCarefully',
    sfen: '8l/6S1k/9/9/8g/9/9/9/9 b GS2r2b2g2s4n3l18p 1',
    nbMoves: 1,
    success: checkmate,
    failure: not(checkmate),
    showFailureMove: 'random',
  },
  {
    goal: 'chooseYourPieceCarefully',
    sfen: '8l/7k1/7pb/7N1/9/9/9/9/9 b RGrb3g4s3n3l17p 1',
    nbMoves: 1,
    success: checkmate,
    failure: not(checkmate),
    showFailureMove: 'random',
  },
  {
    goal: 'chooseYourPieceCarefully',
    sfen: '8l/6+Rgk/7pp/9/9/9/9/9/9 b GSr2b2g3s4n3l16p 1',
    nbMoves: 1,
    success: checkmate,
    failure: not(checkmate),
    showFailureMove: 'random',
  },
  {
    // pawn
    goal: 'mateWithADroppedPawnIs',
    sfen: '7+P1/8g/7pk/9/7Sp/9/9/9/9 b LP2r2b3g3s4n3l14p 1',
    nbMoves: 1,
    success: and(checkmate, not(pieceOn(toPiece('P'), '1d'))),
    failure: or(not(checkmate), pieceOn(toPiece('P'), '1d')),
    drawShapes: initial([circle(toPiece('P'), 'red'), circle(toPiece('L'))]),
    offerIllegalDests: true,
  },
  {
    // pawn 2
    goal: 'mateWithAPushedPawnIs',
    sfen: '7+P1/8g/7pk/9/7SP/9/9/9/9 b 2r2b3g3s4n4l15p 1',
    nbMoves: 1,
    success: checkmate,
    failure: not(checkmate),
    showFailureMove: 'random',
  },
  {
    goal: 'attackYourOpponentsKing',
    sfen: '8l/5BSk1/6p1P/9/9/9/9/9/9 b 2rb4g3s4n3l16p 1',
    nbMoves: 1,
    success: checkmate,
    failure: not(checkmate),
    showFailureMove: 'random',
  },
  {
    goal: 'attackYourOpponentsKing',
    sfen: '5lk2/8R/5Ps2/6N2/6L2/9/9/9/9 b r2b4g3s3n2l17p 1',
    nbMoves: 1,
    success: checkmate,
    failure: not(checkmate),
    showFailureMove: 'random',
  },
  {
    goal: 'attackYourOpponentsKing',
    sfen: '2R3gkl/3R3s1/7p1/7Np/9/9/9/9/9 b 2b3g3s3n3l16p 1',
    nbMoves: 1,
    success: checkmate,
    failure: not(checkmate),
    showFailureMove: 'random',
  },
];

const stage: IncompleteStage = {
  key: 'checkmate1',
  title: 'mateInOne',
  subtitle: 'defeatTheOpponentsKing',
  intro: 'mateInOneIntro',
  levels: levels.map((l, i) => createLevel(l, i)),
  complete: 'mateInOneComplete',
};
export default stage;
