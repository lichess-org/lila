import type { LangsData } from './langs';
import type { BackgroundData } from './background';
import type { BoardData } from './board';
import type { PieceData } from './piece';
import type { DasherCtrl } from './ctrl';
import type { Redraw, VNode } from 'common/snabbdom';

export { DasherCtrl };

export abstract class PaneCtrl {
  constructor(readonly root: DasherCtrl) {}
  get redraw(): Redraw {
    return this.root.redraw;
  }
  get close(): () => void {
    return this.root.close;
  }
  get dimension(): 'd2' | 'd3' {
    return this.root.data.board.is3d ? 'd3' : 'd2';
  }
  get is3d(): boolean {
    return this.root.data.board.is3d;
  }

  abstract render(): VNode;
}

export interface DasherData {
  user?: LightUser;
  lang: LangsData;
  sound: {
    list: string[];
  };
  background: BackgroundData;
  board: BoardData;
  piece: PieceData;
  coach: boolean;
  streamer: boolean;
}

export type Mode = 'links' | 'langs' | 'sound' | 'background' | 'board' | 'piece';

export interface DasherOpts {
  playing: boolean;
  zenable: boolean;
}
