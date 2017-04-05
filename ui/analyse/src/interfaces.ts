import { GameData } from 'game';

export interface AnalyseController {
  study?: Study;
  studyPractice?: StudyPractice;
  socket: Socket;
  vm: Vm;
  jumpToIndex(index: number): void;
  userJumpIfCan(path: Tree.Path): void;
  userJump(path: Tree.Path): void;
  jump(path: Tree.Path): void;

  data: GameData;
  tree: any; // TODO: Tree.Tree;
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
}
