import { LangsData } from './langs';
import { BackgroundData } from './background';
import { BoardData } from './board';
import { PieceData } from './piece';
import { DasherCtrl } from './ctrl';
import { VNode } from 'common/snabbdom';

export { DasherCtrl };

export abstract class PaneCtrl {
  constructor(readonly root: DasherCtrl) {}
  get trans() {
    return this.root.trans;
  }
  get redraw() {
    return this.root.redraw;
  }
  get close() {
    return this.root.close;
  }
  get dimension() {
    return this.root.data.board.is3d ? 'd3' : 'd2';
  }
  get is3d() {
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
  i18n: I18nDict;
}

export type Mode = 'links' | 'langs' | 'sound' | 'background' | 'board' | 'piece';

export interface DasherOpts {
  playing: boolean;
  zenable: boolean;
}
