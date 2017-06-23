import { GameData } from 'game';
import { StoredBooleanProp } from 'common';
import Autoplay from './autoplay';
import { Api as ChessgroundApi } from 'chessground/api';
import { CevalController, NodeEvals } from 'ceval';
import { RetroController } from './retrospect/retroCtrl';

export type MaybeVNode = VNode | null | undefined;
export type MaybeVNodes = MaybeVNode[]

export { Key, Piece } from 'chessground/types';
import { VNode } from 'snabbdom/vnode'

export interface AnalyseController {
  opts: AnalyseOpts;
  redraw: () => void;
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
  getCeval(): CevalController;
  nextNodeBest(): boolean;
  mandatoryCeval(): boolean;
  toggleComputer(): void;
  toggleGauge(): void;
  toggleAutoShapes(v: boolean): void;
  cevalSetInfinite(v: boolean): void;
  cevalSetThreads(v: number): void;
  cevalSetMultiPv(v: number): void;
  cevalSetHashSize(v: number): void;
  encodeNodeFen(): Fen;
  toggleThreatMode(): void;
  toggleCeval(): void;
  gameOver(): boolean;
  currentEvals: () => NodeEvals;
  playUci(uci: Uci): void;
  getOrientation(): Color;
  addNode(node, path): void;
  reset(): void;
  addDests(dests, path, opening): void;
  mergeAnalysisData(data): void;
  evalCache: any;
  autoScroll(): void;
  setAutoShapes(): void;

  trans(key: string): string;

  data: AnalyseData;
  tree: any; // TODO: Tree.Tree;
  userId: string;
  retro: RetroController | null;
  practice: PracticeController | null;
  forecast: ForecastController | null;
  autoplay: Autoplay;
  embed: boolean;
  ongoing: boolean;
  chessground: ChessgroundApi;
  explorer: any; // TODO
  actionMenu: any;
  showEvalGauge(): boolean;
  bottomColor(): Color;
  topColor(): Color;
  mainlinePathToPly(ply: Ply): Tree.Path
}

export interface AnalyseData extends GameData {
  analysis?: any;
}

export interface AnalyseOpts {
  element: Element;
  sideElement: Element;
  data: AnalyseData;
  userId: string;
  embed: boolean;
  explorer: boolean;
  socketSend: any;
}

export interface Study {
  setChapter(id: string): void;
  currentChapter(): StudyChapter;
  data: StudyData;
  socketHandlers: { [key: string]: any };
  vm: any;
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
  threatMode: boolean;
}

export interface PracticeController {
}

export interface ForecastController {
  reloadToLastPly(): void;
}

export type AutoplayDelay = number | 'realtime' | 'cpl_fast' | 'cpl_slow' |
                            'fast' | 'slow';
