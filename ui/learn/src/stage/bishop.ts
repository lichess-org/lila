import { arrow, assetUrl, pieceImg, toLevel } from '../util';
import type { StageNoID } from './list';

const stage: StageNoID = {
  key: 'bishop',
  title: i18n.learn.theBishop,
  subtitle: i18n.learn.itMovesDiagonally,
  image: assetUrl + 'images/learn/pieces/B.svg',
  intro: i18n.learn.bishopIntro,
  illustration: pieceImg('bishop'),
  levels: [
    {
      goal: i18n.learn.grabAllTheStars,
      fen: '8/8/8/8/8/5B2/8/8 w - -',
      apples: 'd5 g8',
      nbMoves: 2,
      shapes: [arrow('f3d5'), arrow('d5g8')],
    },
    {
      goal: i18n.learn.theFewerMoves,
      fen: '8/8/8/8/8/1B6/8/8 w - -',
      apples: 'a2 b1 b5 d1 d3 e2',
      nbMoves: 6,
    },
    {
      goal: i18n.learn.grabAllTheStars,
      fen: '8/8/8/8/3B4/8/8/8 w - -',
      apples: 'a1 b6 c1 e3 g7 h6',
      nbMoves: 6,
    },
    {
      goal: i18n.learn.grabAllTheStars,
      fen: '8/8/8/8/2B5/8/8/8 w - -',
      apples: 'a4 b1 b3 c2 d3 e2',
      nbMoves: 6,
    },
    {
      goal: i18n.learn.youNeedBothBishops,
      fen: '8/8/8/8/8/8/8/2B2B2 w - -',
      apples: 'd3 d4 d5 e3 e4 e5',
      nbMoves: 6,
    },
    {
      goal: i18n.learn.youNeedBothBishops,
      fen: '8/3B4/8/8/8/2B5/8/8 w - -',
      apples: 'a3 c2 e7 f5 f6 g8 h4 h7',
      nbMoves: 11,
    },
  ].map(toLevel),
  complete: i18n.learn.bishopComplete,
};
export default stage;
