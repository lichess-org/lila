import { pieceOn, scenarioFailure, scenarioSuccess } from '../assert';
import { IncompleteLevel, IncompleteStage } from '../interfaces';
import { createLevel } from '../level';
import { arrow, circle, concat, initial, onPly } from '../shapes';
import { createScenario } from '../util';

const levels: IncompleteLevel[] = [
  {
    goal: 'thisIsTheInitialPosition',
    sfen: 'lnsgkgsnl/1r5b1/ppppppppp/9/9/9/PPPPPPPPP/1B5R1/LNSGKGSNL b - 1',
    nbMoves: 1,
    success: () => true,
  },
  {
    goal: 'firstPlaceTheKing',
    sfen: '4k4/9/9/9/9/9/9/6K2/9 b RB2G2S2N2L9Prb2g2s2n2l9p 1',
    nbMoves: 2,
    success: pieceOn({ role: 'king', color: 'sente' }, '5i'),
    drawShapes: initial([circle('5i')]),
  },
  {
    goal: 'dropAGold',
    sfen: '4k4/9/9/9/9/9/9/9/4K4 b RB2G2S2N2L9Prb2g2s2n2l9p 1',
    nbMoves: 2,
    success: scenarioSuccess,
    failure: scenarioFailure,
    drawShapes: concat(initial([circle('6i')]), onPly(2, [circle('4i')])),
    scenario: createScenario(['G*6i', 'G*4a', 'G*4i', 'G*6a'], 'sente', true),
  },
  {
    goal: 'thenPlaceASilver',
    sfen: '3gkg3/9/9/9/9/9/9/9/3GKG3 b RB2S2N2L9Prb2s2n2l9p 1',
    nbMoves: 2,
    success: scenarioSuccess,
    failure: scenarioFailure,
    drawShapes: initial([circle('7i')]),
    scenario: createScenario(['S*7i', 'S*7a', 'S*3i', 'S*3a'], 'sente', true),
  },
  {
    goal: 'dropTheKnights',
    sfen: '2sgkgs2/9/9/9/9/9/9/9/2SGKGS2 b RB2N2L9Prb2n2l9p 1',
    nbMoves: 2,
    success: scenarioSuccess,
    failure: scenarioFailure,
    scenario: createScenario(['N*8i', 'N*2a', 'N*2i', 'N*8a'], 'sente', true),
  },
  {
    goal: 'dropTheLances',
    sfen: '1nsgkgsn1/9/9/9/9/9/9/9/1NSGKGSN1 b RB2L9Prb2l9p 1',
    nbMoves: 2,
    success: scenarioSuccess,
    failure: scenarioFailure,
    scenario: createScenario(['L*9i', 'P*1c', 'L*1i', 'P*2c'], 'sente', true),
  },
  {
    goal: 'placeTheBishopThenRook',
    sfen: '1nsgkgsn1/9/7pp/9/9/9/9/9/LNSGKGSNL b RB9Prb2l7p 1',
    nbMoves: 2,
    success: scenarioSuccess,
    failure: scenarioFailure,
    scenario: createScenario(['B*8h', 'P*3c', 'R*2h', 'P*4c'], 'sente', true),
  },
  {
    goal: 'placeThePawns',
    sfen: '1nsgkgsn1/9/5pppp/9/9/9/9/1B5R1/LNSGKGSNL b 9Prb2l5p 1',
    nbMoves: 17,
    success: scenarioSuccess,
    failure: scenarioFailure,
    drawShapes: concat(
      initial([circle('5g')]),
      onPly(2, [circle('6g')]),
      onPly(4, [circle('4g')]),
      onPly(6, [circle('7g')]),
      onPly(8, [circle('3g')]),
      onPly(10, [circle('8g')]),
      onPly(12, [circle('2g')]),
      onPly(14, [circle('9g')]),
      onPly(16, [circle('1g')])
    ),
    scenario: createScenario(
      [
        'P*5g',
        'P*5c',
        'P*6g',
        'P*6c',
        'P*4g',
        'P*7c',
        'P*7g',
        'P*8c',
        'P*3g',
        'P*9c',
        'P*8g',
        'L*1a',
        'P*2g',
        'L*9a',
        'P*9g',
        'B*2b',
        'P*1g',
        'R*8b',
      ],
      'sente',
      true
    ),
  },
  {
    goal: 'pushingThe3rdPawn',
    sfen: 'lnsgkgsnl/1r5b1/ppppppppp/9/9/9/PPPPPPPPP/1B5R1/LNSGKGSNL b - 1',
    nbMoves: 2,
    success: scenarioSuccess,
    failure: scenarioFailure,
    drawShapes: initial([arrow('7g', '7f')]),
    scenario: createScenario(['7g7f'], 'sente', false),
  },
];

const stage: IncompleteStage = {
  key: 'setup',
  title: 'boardSetup',
  subtitle: 'howTheGameStarts',
  intro: 'boardSetupIntro',
  levels: levels.map((l, i) => createLevel(l, i)),
  complete: 'boardSetupComplete',
};
export default stage;
