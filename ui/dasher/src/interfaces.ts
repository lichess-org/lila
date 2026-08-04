import { debounce, hyphenToCamel } from 'lib';
import type { VNode } from 'lib/view';
import { text as xhrText } from 'lib/xhr';

import type { DasherCtrl } from '@/ctrl';
import type { LangsData } from '@/langs';
import type { BackgroundData } from '@/theme';

export type Dimension = 'd2' | 'd3';

export abstract class PaneCtrl {
  constructor(readonly root: DasherCtrl) {}
  get redraw(): Redraw {
    return this.root.redraw;
  }
  get close(): () => void {
    return this.root.close;
  }
  get dimension(): Dimension {
    return this.root.data.board.is3d ? 'd3' : 'd2';
  }
  get is3d(): boolean {
    return this.root.data.board.is3d;
  }

  protected readonly getVar = (prop: string): number =>
    parseInt(window.getComputedStyle(document.body).getPropertyValue(`---${prop}`));

  protected readonly postPref: (prop: string) => void = debounce((prop: string) => {
    const failed = () => site.announce({ msg: `Failed to save ${prop}` });
    if (prop === 'zoom')
      return xhrText(`/pref/zoom?v=${this.getVar('zoom')}`, { method: 'post' }).catch(failed);

    const body = new FormData();
    body.set(hyphenToCamel(prop), this.getVar(prop).toString());
    const path = `/pref/${hyphenToCamel(prop)}`;
    return xhrText(path, { body, method: 'post' }).catch(failed);
  }, 1000);

  abstract render(): VNode;
}

type BoardAsset = { name: string; file?: string; featured: boolean };
type AssetData = Record<Dimension, { current: string; list: BoardAsset[] }>;

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

export type Mode = 'links' | 'langs' | 'sound' | 'theme' | 'board' | 'piece';

export interface DasherOpts {
  playing: boolean;
  zenable: boolean;
}

export type Range = { min: number; max: number; step: number };
