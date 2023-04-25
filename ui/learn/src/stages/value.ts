import { parseSquareName, parseUsi } from 'shogiops/util';
import { colorOn, extinct, not, pieceOn, scenarioFailure, scenarioSuccess } from '../assert';
import { IncompleteLevel, IncompleteStage, Level, UsiWithColor } from '../interfaces';
import { createLevel } from '../level';
import { arrow, circle, initial, onFailure } from '../shapes';
import { findRandomMove } from '../shogi';
import { createScenario, currentPosition, toPiece } from '../util';

const notSuccess = () => true;

const levels: IncompleteLevel[] = [
  {
    goal: 'pawnsAreTheLeastValuable',
    sfen: '9/9/3pl4/2n1+R4/9/9/9/9/9 b - 1',
    nbMoves: 1,
    success: colorOn('sente', '7d'),
    failure: notSuccess,
  },
  {
    goal: 'knightSilverGold',
    sfen: '9/9/4ns3/4S4/3g5/9/9/9/9 b - 1',
    nbMoves: 1,
    success: colorOn('sente', '4c'),
    failure: notSuccess,
    showFailureMove: 'unprotected',
  },
  {
    goal: 'goldBishopRook',
    sfen: '9/9/9/5gb2/4p+Bn2/4srl2/4gs3/9/9 b - 1',
    nbMoves: 1,
    success: colorOn('sente', '4f'),
    failure: notSuccess,
  },
  {
    goal: 'takeAllThePiecesStartingFromTheMost',
    sfen: '9/9/9/3lg4/9/3+R1n3/2r1bp3/2s6/9 b - 1',
    nbMoves: 11,
    success: extinct('gote'),
    failure: (_level: Level, usiCList: UsiWithColor[]) => {
      const dests = usiCList.map(uc => parseUsi(uc.usi)!.to),
        targets = ['7g', '5g', '5d', '7h', '4f', '6d', '4g'].map(sq => parseSquareName(sq)!);
      for (const d of dests) {
        if (targets[0] === d) targets.shift();
        else if (targets.includes(d)) return true;
      }
      return false;
    },
  },
  {
    goal: 'anExchangeIs',
    sfen: '9/6k2/3p2g2/3s5/7N1/9/9/3R5/9 b - 1',
    nbMoves: 1,
    success: colorOn('sente', '3c'),
    failure: notSuccess,
  },
  {
    goal: 'theOpponentJustGaveAway',
    sfen: '7k1/3r5/6+P+P+P/9/P3n4/2N6/2P6/2K6/L8 b 2g 1',
    color: 'sente',
    nbMoves: 2,
    success: scenarioSuccess,
    failure: scenarioFailure,
    scenario: createScenario(['6b6h+', '7h8g'], 'gote', true),
    drawShapes: onFailure([circle(toPiece('g'))]),
    showFailureMove: (_, usiCList) => {
      const lastUsiC = usiCList[usiCList.length - 1];
      if (lastUsiC?.usi === '7h6h') return 'G*6g';
      else return 'G*8h';
    },
  },
  {
    goal: 'yourKingsValueIsInfinite',
    sfen: '7k1/9/6+P+P+P/9/Pg2n4/2N6/1KP6/3+r5/L2G5 b g 1',
    nbMoves: 1,
    success: pieceOn({ role: 'tokin', color: 'sente' }, '2b'),
    failure: not(pieceOn({ role: 'tokin', color: 'sente' }, '2b')),
    drawShapes: onFailure([arrow(toPiece('g'), '8f')]),
    showFailureMove: (level, usiCList: UsiWithColor[]) => {
      const lastUsiC = usiCList[usiCList.length - 1];
      if (['3c3b', '2c3b', '2c1b', '1c1b'].includes(lastUsiC?.usi)) {
        const pos = currentPosition(level, usiCList);
        return findRandomMove(pos);
      } else return 'G*8f';
    },
  },
  {
    goal: 'twoGeneralsAreBetter',
    sfen: 'l6rl/2k3g2/2ppps3/pp3pp2/2P4pp/P1BP1P3/1P2P1PPP/2S4R1/L1K2G2L b b 1',
    nbMoves: 3,
    success: scenarioSuccess,
    failure: scenarioFailure,
    scenario: createScenario(['7f4c+', '3b4c', 'S*3b', '2a2b', '3b4c+'], 'sente', true),
  },
  {
    goal: 'rememberWhichPieceIsTheMostValuable',
    sfen: 'ln+R5l/3r1gks1/5pnp1/2b3p1p/1p7/P3pP2P/1P1+b1GPP1/6SK1/5+p1NL b GSPgsnl3p 1',
    nbMoves: 1,
    success: scenarioSuccess,
    failure: scenarioFailure,
    drawShapes: initial([
      arrow('7a', '7d'),
      arrow('7a', '6b'),
      arrow('7a', '8a'),
      circle('7d'),
      circle('6b'),
      circle('8a'),
    ]),
    scenario: createScenario(['S*2a']),
  },
];

const stage: IncompleteStage = {
  key: 'value',
  title: 'pieceValue',
  subtitle: 'evaluatePieceStrength',
  intro: 'pieceValueIntroNew',
  levels: levels.map((l, i) => createLevel(l, i)),
  complete: 'rememberTheKingIsMoreValuableThanEveryOtherPieceCombined',
};
export default stage;
