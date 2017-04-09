import { GameData } from 'game';
import { StoredBooleanProp } from 'common';

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

  trans(key: string): string;

  data: GameData;
  tree: any; // TODO: Tree.Tree;
  retro: RetroController | null;
}

export interface AnalyseOpts {
  element: Element;
  sideElement: Element;
}

export interface Study {
  setChapter(id: string): void;
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
}

export interface RetroController {
}
