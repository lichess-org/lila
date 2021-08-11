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
import { MNode } from '../mithrilFix';
import { Shape } from '../ground';
import { AssertData } from '../level';
import { ScenarioLevel } from '../scenario';
import type { Square as Key } from 'chess.js';

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
  apples: string | Key[];
  color: 'black' | 'white';
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
  illustration: MNode;
  levels: Level[];
  cssClass?: string;
}

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
    name: 'chessPieces',
    stages: [rook, bishop, queen, king, knight, pawn],
  },
  {
    key: 'fundamentals',
    name: 'fundamentals',
    stages: [capture, protection, combat, check1, outOfCheck, checkmate1],
  },
  {
    key: 'intermediate',
    name: 'intermediate',
    stages: [setup, castling, enpassant, stalemate],
  },
  {
    key: 'advanced',
    name: 'advanced',
    stages: [
      value,
      // draw,
      // fork,
      check2,
    ],
  },
];

let stageId = 1;
const stages: Stage[] = [];

export const categs: Categ[] = rawCategs.map(function (c) {
  c.stages = c.stages.map(function (frozenStage) {
    // Module exports get frozen, so we copy them to be able to mutate them
    const stage = { ...frozenStage, id: stageId++ };
    stages.push(stage);
    return stage;
  });
  return c as Categ;
});

const stagesByKey: { [K in string]: Stage } = {};
stages.forEach(function (s) {
  stagesByKey[s.key] = s;
});

const stagesById: { [K in number]: Stage } = {};
stages.forEach(function (s) {
  stagesById[s.id!] = s;
});

export const list = stages;
export const byId = stagesById;
export const byKey = stagesByKey;

export function stageIdToCategId(stageId: number): number | undefined {
  const stage = stagesById[stageId];
  for (let id = 0; id < categs.length; id++)
    if (
      categs[id].stages.some(function (s) {
        return s.key === stage.key;
      })
    )
      return id;
  return;
}
