import { anyCapture, not, unprotectedCapture } from '../assert';
import { IncompleteLevel, IncompleteStage } from '../interfaces';
import { createLevel } from '../level';
import { arrow, concat, initial, onSuccess } from '../shapes';

const levels: IncompleteLevel[] = [
  {
    goal: 'escape',
    sfen: '9/1r7/9/9/9/2PP5/Pp7/1B7/LNS6 b - 1',
    nbMoves: 1,
    success: not(anyCapture),
    failure: anyCapture,
    drawShapes: initial([arrow('8g', '8h', 'red')]),
    showFailureMove: 'capture',
  },
  {
    // escape
    goal: 'escape',
    sfen: '9/9/9/9/7Pb/9/8P/6+p2/3S3RL b - 1',
    nbMoves: 1,
    success: not(anyCapture),
    failure: anyCapture,
    showFailureMove: 'capture',
  },
  {
    // protect
    goal: 'noEscape',
    sfen: '9/9/2n6/2r6/4P4/2S6/1PBP5/2R6/9 b - 1',
    nbMoves: 3,
    success: not(unprotectedCapture),
    failure: unprotectedCapture,
    drawShapes: concat(initial([arrow('7d', '7f', 'red'), arrow('7h', '7g')]), onSuccess([arrow('7h', '7f')])),
    showFailureMove: 'unprotected',
  },
  {
    goal: 'makeSureAllSafe',
    sfen: '9/9/6b2/7R1/9/9/1P7/1S7/9 b - 1',
    nbMoves: 1,
    success: not(unprotectedCapture),
    failure: unprotectedCapture,
    drawShapes: concat(initial([arrow('3c', '2d', 'red'), arrow('3c', '8h', 'red')]), onSuccess([arrow('2h', '8h')])),
    showFailureMove: 'unprotected',
  },
  {
    goal: 'makeSureAllSafe',
    sfen: '9/9/6n2/9/9/5Pp2/6N2/9/9 b - 1',
    nbMoves: 1,
    success: not(unprotectedCapture),
    failure: unprotectedCapture,
    showFailureMove: 'unprotected',
  },
  {
    goal: 'makeSureAllSafe',
    sfen: '9/9/7+P1/6s2/5N3/9/9/9/9 b - 1',
    nbMoves: 1,
    success: not(unprotectedCapture),
    failure: unprotectedCapture,
    showFailureMove: 'unprotected',
  },
  {
    goal: 'makeSureAllSafe',
    sfen: '9/9/9/9/9/9/5G3/9/3+bS4 b - 1',
    nbMoves: 1,
    success: not(unprotectedCapture),
    failure: unprotectedCapture,
    showFailureMove: 'unprotected',
  },
  {
    goal: 'makeSureAllSafe',
    sfen: '9/9/9/9/3n5/9/2PPS4/3R5/9 b - 1',
    nbMoves: 1,
    success: not(unprotectedCapture),
    failure: unprotectedCapture,
    showFailureMove: 'unprotected',
  },
  {
    goal: 'dontForgetYouCanDropToDefend',
    sfen: '9/9/4S4/9/4r4/9/4G4/9/9 b L 1',
    nbMoves: 1,
    success: not(unprotectedCapture),
    failure: unprotectedCapture,
    showFailureMove: 'unprotected',
  },
  {
    goal: 'dontForgetYouCanDropToDefend',
    sfen: '9/9/9/9/9/3B5/7S1/9/3G3r1 b NLP 1',
    nbMoves: 1,
    success: not(unprotectedCapture),
    failure: unprotectedCapture,
    showFailureMove: 'unprotected',
  },
  {
    goal: 'dontLetThemTakeAnyUndefendedPiece',
    sfen: '9/9/9/9/9/9/4P4/4G4/2S2+r3 b NLP 1',
    nbMoves: 1,
    success: not(unprotectedCapture),
    failure: unprotectedCapture,
    showFailureMove: 'unprotected',
  },
  {
    goal: 'dontLetThemTakeAnyUndefendedPiece',
    sfen: '9/9/9/9/7S1/7+b1/9/5G3/9 b NLP 1',
    nbMoves: 1,
    success: not(unprotectedCapture),
    failure: unprotectedCapture,
    showFailureMove: 'unprotected',
  },
  {
    goal: 'dontLetThemTakeAnyUndefendedPiece',
    sfen: '8l/9/9/7n1/8P/8L/6PP1/6S2/7N1 b LP 1',
    nbMoves: 1,
    success: not(unprotectedCapture),
    failure: unprotectedCapture,
    showFailureMove: 'unprotected',
  },
];

const stage: IncompleteStage = {
  key: 'protection',
  title: 'protection',
  subtitle: 'keepYourPiecesSafe',
  intro: 'protectionIntro',
  levels: levels.map((l, i) => createLevel(l, i)),
  complete: 'protectionComplete',
};
export default stage;
