import { and, check, colorOn, not, pieceOn, unprotectedCapture } from '../assert';
import { IncompleteLevel, IncompleteStage } from '../interfaces';
import { createLevel } from '../level';
import { arrow, checkShapes, circle, concat, initial, onUsi } from '../shapes';
import { toPiece } from '../util';

const levels: IncompleteLevel[] = [
  {
    goal: 'ifYourKingIsAttacked',
    sfen: '9/9/9/9/4r4/9/9/9/4K4 b - 1',
    nbMoves: 1,
    success: not(check('sente')),
    failure: () => true,
    drawShapes: concat(initial([arrow('5e', '5i', 'red')]), checkShapes),
    offerIllegalDests: true,
  },
  {
    goal: 'escapeWithTheKing',
    sfen: '9/9/9/9/9/9/9/2s1r4/3K5 b - 1',
    nbMoves: 1,
    success: not(check('sente')),
    failure: () => true,
    drawShapes: checkShapes,
    offerIllegalDests: true,
  },
  {
    goal: 'theKingCannotEscapeButBlock',
    sfen: '9/9/9/9/9/9/6PPP/4+r2K1/6SNL b - 1',
    nbMoves: 1,
    success: not(check('sente')),
    failure: () => true,
    drawShapes: checkShapes,
    offerIllegalDests: true,
  },
  {
    goal: 'theKingCannotEscapeButBlock',
    sfen: '9/9/9/9/9/9/6PPP/5+r1K1/7NL b G 1',
    nbMoves: 1,
    success: not(check('sente')),
    failure: () => true,
    drawShapes: checkShapes,
    offerIllegalDests: true,
  },
  {
    goal: 'theKingCannotEscapeButBlock',
    sfen: '9/9/9/9/5b3/9/6B1P/8K/6sNL b P 1',
    nbMoves: 1,
    success: not(check('sente')),
    failure: () => true,
    drawShapes: checkShapes,
    offerIllegalDests: true,
  },
  {
    goal: 'youCanGetOutOfCheckByTaking',
    sfen: '9/9/9/9/9/9/6PPp/5+p1KP/5GbNL b - 1',
    nbMoves: 1,
    success: not(check('sente')),
    failure: () => true,
    drawShapes: checkShapes,
    offerIllegalDests: true,
  },
  {
    goal: 'getOutOfCheck',
    sfen: '9/9/9/9/9/9/3Ss4/3s5/3K5 b - 1',
    nbMoves: 1,
    success: not(check('sente')),
    failure: () => true,
    drawShapes: checkShapes,
    offerIllegalDests: true,
  },
  {
    goal: 'getOutOfCheck',
    sfen: '9/9/9/7p1/8b/4sPK2/6g2/9/9 b - 1',
    nbMoves: 1,
    success: not(check('sente')),
    failure: () => true,
    drawShapes: checkShapes,
    offerIllegalDests: true,
  },
  {
    goal: 'getOutOfCheck',
    sfen: '9/9/9/6n2/6p2/5Ks2/9/4r4/9 b - 1',
    nbMoves: 1,
    success: not(check('sente')),
    failure: () => true,
    drawShapes: checkShapes,
    offerIllegalDests: true,
  },
  {
    goal: 'getOutOfCheck',
    sfen: '9/9/9/9/8P/8l/7P1/6rSK/7NL b P 1',
    nbMoves: 1,
    success: and(not(check('sente')), not(pieceOn(toPiece('P'), '1g'))),
    failure: () => true,
    drawShapes: concat(checkShapes, onUsi('P*1g', [circle('1e', 'red'), circle('1g', 'red')])),
    offerIllegalDests: true,
  },
  {
    goal: 'watchOutForYourOpponentsReply',
    sfen: '9/9/9/9/9/4b4/PPG6/LS7/K4r3 b L 1',
    nbMoves: 1,
    success: and(not(unprotectedCapture), not(check('sente'))),
    failure: () => true,
    drawShapes: checkShapes,
    offerIllegalDests: true,
    showFailureMove: 'unprotected',
  },
  {
    goal: 'watchOutForYourOpponentsReply',
    sfen: '9/9/9/9/9/5+bn2/5GPPP/6SK1/5G1NL b g 1',
    nbMoves: 1,
    success: and(not(check('sente')), colorOn('sente', '3f')),
    failure: () => true,
    drawShapes: checkShapes,
    offerIllegalDests: true,
    showFailureMove: (_, usiCList) => {
      if (usiCList.length && usiCList[0].usi.startsWith('2h')) return 'G*2h';
      else return undefined;
    },
  },
];
const stage: IncompleteStage = {
  key: 'outOfCheck',
  title: 'outOfCheck',
  subtitle: 'defendYourKing',
  intro: 'outOfCheckIntro',
  levels: levels.map((l, i) => createLevel(l, i)),
  complete: 'outOfCheckComplete',
};

export default stage;
