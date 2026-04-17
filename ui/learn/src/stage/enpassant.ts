import { scenarioComplete, scenarioFailed } from '../assert';
import { arrow, assetUrl, roundSvg, toLevel } from '../util';
import type { StageNoID } from './list';

const imgUrl = assetUrl + 'images/learn/spinning-blades.svg';

const stage: StageNoID = {
  key: 'enpassant',
  title: i18n.site.enPassant,
  subtitle: i18n.learn.theSpecialPawnMove,
  image: imgUrl,
  intro: i18n.learn.enPassantIntro,
  illustration: roundSvg(imgUrl),
  levels: [
    {
      goal: i18n.learn.blackJustMovedThePawnByTwoSquares,
      fen: 'rnbqkbnr/pppppppp/8/2P5/8/8/PP1PPPPP/RNBQKBNR b KQkq -',
      color: 'white' as const,
      nbMoves: 1,
      success: scenarioComplete,
      failure: scenarioFailed,
      detectCapture: false,
      scenario: [
        {
          move: 'd7d5',
          shapes: [arrow('c5d6')],
        },
        'c5d6',
      ],
      captures: 1,
    },
    {
      goal: i18n.learn.enPassantOnlyWorksImmediately,
      fen: 'rnbqkbnr/ppp1pppp/8/2Pp3P/8/8/PP1PPPP1/RNBQKBNR b KQkq -',
      color: 'white' as const,
      nbMoves: 1,
      success: scenarioComplete,
      failure: scenarioFailed,
      detectCapture: false,
      scenario: [
        {
          move: 'g7g5',
          shapes: [arrow('h5g6'), arrow('c5d6', 'red')],
        },
        'h5g6',
      ],
      captures: 1,
    },
    {
      goal: i18n.learn.enPassantOnlyWorksOnFifthRank,
      fen: 'rnbqkbnr/pppppppp/P7/2P5/8/8/PP1PPPP1/RNBQKBNR b KQkq -',
      color: 'white' as const,
      nbMoves: 1,
      success: scenarioComplete,
      failure: scenarioFailed,
      detectCapture: false,
      scenario: [
        {
          move: 'b7b5',
          shapes: [arrow('c5b6'), arrow('a6b7', 'red')],
        },
        'c5b6',
      ],
      captures: 1,
      cssClass: 'highlight-5th-rank',
    },
    {
      goal: i18n.learn.takeAllThePawnsEnPassant,
      fen: 'rnbqkbnr/pppppppp/8/2PPP2P/8/8/PP1P1PP1/RNBQKBNR b KQkq -',
      color: 'white' as const,
      nbMoves: 4,
      detectCapture: false,
      success: scenarioComplete,
      failure: scenarioFailed,
      scenario: ['b7b5', 'c5b6', 'f7f5', 'e5f6', 'c7c5', 'd5c6', 'g7g5', 'h5g6'],
      captures: 4,
    },
  ].map(toLevel),
  complete: i18n.learn.enPassantComplete,
};
export default stage;
