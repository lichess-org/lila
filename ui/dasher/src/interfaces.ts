import type { LangsData } from './langs';
import type { BackgroundData } from './background';
import type { DasherCtrl } from './ctrl';
import type { VNode } from 'lib/snabbdom';

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

type BoardAsset = { name: string; file?: string; featured: boolean };
type AssetData = { [key in 'd2' | 'd3']: { current: string; list: BoardAsset[] } };

export interface DasherData {
  user?: LightUser;
  lang: LangsData;
  sound: {
    list: string[];
  };
  background: BackgroundData;
  board: AssetData & { is3d: boolean };
  piece: AssetData;
  coach: boolean;
  streamer: boolean;
}

export type Mode = 'links' | 'langs' | 'sound' | 'background' | 'board' | 'piece';

export interface DasherOpts {
  playing: boolean;
  zenable: boolean;
}
