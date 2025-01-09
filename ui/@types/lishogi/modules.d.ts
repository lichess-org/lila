export interface LishogiModules {
  challenge?: (opts: any) => { update: (d: any) => any };
  dasher?: (opts: { playing: boolean }) => Promise<any>;
  keyboardMove?: (opts: any) => any;
  miscCli?: (opts: { $wrap: JQuery; toggle: () => void }) => any;
  miscExpandText?: () => void;
  miscMusic: () => { jump: (node: Tree.Node) => void };
  notify?: (opts: any) => any;
  palantir: (opts: any) => void | { render: (h: any) => any };
  speech?: (opts: LishogiSpeech) => any;
  chartAcpl?: (
    el: HTMLCanvasElement,
    data: any,
    mainline: Tree.Node[],
  ) => {
    updateData: (data: any, mainline: Tree.Node[]) => void;
    selectPly: (ply: number, isMainline: boolean) => void;
  };
  chartMovetime?: (
    el: HTMLCanvasElement,
    data: any,
    hunter: boolean,
  ) => {
    selectPly: (ply: number, isMainline: boolean) => void;
  };
  analyseNvui?: LishogiNvui;
  roundNvui?: LishogiNvui;
}
