export interface AnalyseController {
  study?: Study;
  studyPractice?: StudyPractice;
  socket: Socket;
  vm: Vm;
  jumpToIndex(index: number): void;
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
}
