import rook from './rook';
import bishop from './bishop';
import queen from './queen';
import king from './king';
import knight from './knight';
import pawn from './pawn';
import capture from './capture';
import protection from './protection';
import combat from './combat';
import check1 from './check1';
import outOfCheck from './outOfCheck';
import checkmate1 from './checkmate1';
import setup from './setup';
import castling from './castling';
import enpassant from './enpassant';
import stalemate from './stalemate';
import value from './value';
import check2 from './check2';
import type { AssertData } from '../levelCtrl';
import type { ScenarioLevel } from '../scenario';
import type { SquareName } from 'chessops';
import type { VNode } from 'snabbdom';
import type { Shape } from '../chessground';

export type Level = LevelBase & LevelDefaults;
export type LevelPartial = LevelBase & Partial<LevelDefaults>;

export interface LevelBase {
  goal: string;
  fen: string;
  nbMoves: number;

  success?(data: AssertData): boolean;
  failure?(data: AssertData): boolean;
  scenario?: ScenarioLevel;
  shapes?: Shape[];

  autoCastle?: boolean;
  captures?: number;
  cssClass?: string;
  emptyApples?: boolean;
  explainPromotion?: boolean;
  nextButton?: boolean;
  offerIllegalMove?: boolean;
  pointsForCapture?: boolean;
  showPieceValues?: boolean;
  showFailureFollowUp?: boolean;
}

export interface LevelDefaults {
  id: number;
  apples: string | SquareName[];
  color: Color;
  detectCapture: 'unprotected' | boolean;
}

export interface Stage {
  id: number;
  key: string;
  title: string;
  subtitle: string;
  image: string;
  intro: string;
  complete: string;
  illustration: VNode;
  levels: Level[];
  cssClass?: string;
}

export type StageNoID = Omit<Stage, 'id'>;

interface Categ {
  key: string;
  name: string;
  stages: Stage[];
}

interface RawCateg {
  key: string;
  name: string;
  stages: Omit<Stage, 'id'>[];
}

const rawCategs: RawCateg[] = [
  {
    key: 'chess-pieces',
    name: i18n.learn.chessPieces,
    stages: [rook, bishop, queen, king, knight, pawn],
  },
  {
    key: 'fundamentals',
    name: i18n.learn.fundamentals,
    stages: [capture, protection, combat, check1, outOfCheck, checkmate1],
  },
  {
    key: 'intermediate',
    name: i18n.learn.intermediate,
    stages: [setup, castling, enpassant, stalemate],
  },
  {
    key: 'advanced',
    name: i18n.learn.advanced,
    stages: [
      value,
      // draw,
      // fork,
      check2,
    ],
  },
];

let stageId = 1;
export const categs: Categ[] = rawCategs.map(c => ({
  ...c,
  stages: c.stages.map(s => ({ ...s, id: stageId++ })),
}));

const stages: Stage[] = categs.reduce<Stage[]>((prev, c) => prev.concat(c.stages), []);

const stagesByKey: { [K in string]: Stage } = Object.fromEntries(stages.map(s => [s.key, s]));

const stagesById: { [K in number]: Stage } = Object.fromEntries(stages.map(s => [s.id, s]));

export const list = stages;
export const byId = stagesById;
export const byKey = stagesByKey;

export function stageIdToCategId(stageId: number): number | undefined {
  const stage = stagesById[stageId];
  const maybeFound = categs.findIndex(c => c.stages.some(s => s.key === stage.key));
  return maybeFound >= 0 ? maybeFound : undefined;
}
