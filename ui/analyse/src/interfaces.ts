import { GameData } from 'game';
import { StoredBooleanProp } from 'common';
import Autoplay from './autoplay';
import { Api as ChessgroundApi } from 'chessground/api';
import { CevalController } from 'ceval';

export interface AnalyseController {
  study?: Study;
  studyPractice?: StudyPractice;
  socket: Socket;
  vm: Vm;
  jumpToIndex(index: number): void;
  userJumpIfCan(path: Tree.Path): void;
  userJump(path: Tree.Path): void;
  jump(path: Tree.Path): void;
  toggleRetro(): void;
  jumpToGlyphSymbol(color: Color, symbol: string): void;
  togglePlay(delay: AutoplayDelay): void;
  flip(): void;
  getCeval(): CevalController | undefined;
  mandatoryCeval(): boolean;
  toggleComputer(): void;
  toggleGauge(): void;
  toggleAutoShapes(v: boolean): void;
  cevalSetInfinite(v: boolean): void;
  cevalSetThreads(v: number): void;
  cevalSetMultiPv(v: number): void;
  cevalSetHashSize(v: number): void;
  encodeNodeFen(): string;

  trans(key: string): string;

  data: GameData;
  tree: any; // TODO: Tree.Tree;
  userId: string;
  retro: RetroController | null;
  autoplay: Autoplay;
  embed: boolean;
  ongoing: boolean;
  chessground: ChessgroundApi;
}

export interface AnalyseOpts {
  element: Element;
  sideElement: Element;
}

export interface Study {
  setChapter(id: string): void;
  currentChapter(): StudyChapter;
  data: StudyData;
}

export interface StudyData {
  id: string;
}

export interface StudyChapter {
  id: string;
}

export interface StudyPractice {
}

export interface Socket {
  receive(type: string, data: any): void;
}

export interface Vm {
  path: Tree.Path;
  node: Tree.Node;
  mainline: Tree.Node[];
  onMainline: boolean;
  nodeList: Tree.Node[];
  showComputer: StoredBooleanProp;
  showAutoShapes: StoredBooleanProp;
  showGauge: StoredBooleanProp;
}

export interface RetroController {
}

export type AutoplayDelay = number | 'realtime' | 'cpl_fast' | 'cpl_slow' |
                            'fast' | 'slow';
