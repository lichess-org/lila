import type { LevelPartial, StageNoID } from './list';
import { arrow, assetUrl, pieceImg, toLevel } from '../util';

const stage: StageNoID = {
  key: 'king',
  title: i18n.learn.theKing,
  subtitle: i18n.learn.theMostImportantPiece,
  image: assetUrl + 'images/learn/pieces/K.svg',
  intro: i18n.learn.kingIntro,
  illustration: pieceImg('king'),
  levels: [
    {
      goal: i18n.learn.theKingIsSlow,
      fen: '8/8/8/8/8/3K4/8/8 w - -',
      apples: 'e6',
      nbMoves: 3,
      shapes: [arrow('d3d4'), arrow('d4d5'), arrow('d5e6')],
    },
    {
      goal: i18n.learn.grabAllTheStars,
      fen: '8/8/8/8/8/8/8/4K3 w - -',
      apples: 'c2 d3 e2 e3',
      nbMoves: 4,
    },
    {
      goal: i18n.learn.lastOne,
      fen: '8/8/8/4K3/8/8/8/8 w - -',
      apples: 'b5 c5 d6 e3 f3 g4',
      nbMoves: 8,
    },
  ].map((l: LevelPartial, i) => toLevel({ ...l, emptyApples: true }, i)),
  complete: i18n.learn.kingComplete,
};
export default stage;
