import {
  init,
  attributesModule,
  eventListenersModule,
  classModule,
  propsModule,
  styleModule,
  type VNode,
} from 'snabbdom';

import { Coords } from 'lib/prefs';
import { pubsub } from 'lib/pubsub';

import { LearnCtrl } from './ctrl';
import storage, { type Storage } from './storage';
import { view } from './view';

interface NvuiPlugin {
  render(): VNode;
}

const patch = init([classModule, attributesModule, propsModule, eventListenersModule, styleModule]);

export interface LearnProgress {
  _id?: string;
  stages: Record<string, StageProgress>;
}

export interface StageProgress {
  scores: number[];
}

export interface LearnOpts {
  storage: Storage;
  stageId: number | null;
  levelId: number | null;
  route?: string;
  pref: LearnPrefs;
}

export interface LearnPrefs {
  coords: Coords;
  destination: boolean;
  is3d: boolean;
}

interface LearnServerOpts {
  data?: LearnProgress;
  pref: LearnPrefs;
}

export async function initModule({ data, pref }: LearnServerOpts) {
  const _storage = storage(data);
  const opts: LearnOpts = {
    storage: _storage,
    stageId: null,
    levelId: null,
    pref,
  };
  const ctrl = new LearnCtrl(opts, redraw);

  const nvui = site.blindMode && (await site.asset.loadEsm<NvuiPlugin>('learn.nvui', { init: ctrl }));
  const render = nvui ? () => nvui.render() : () => view(ctrl);

  const element = document.getElementById('learn-app')!;
  element.innerHTML = '';
  const inner = document.createElement('div');
  element.appendChild(inner);
  let vnode = patch(inner, render());

  function redraw() {
    vnode = patch(vnode, render());
  }

  redraw();

  const was3d = document.head.querySelector(`link[data-css-key='lib.board-3d']`) !== null;
  pubsub.on('board.change', (is3d: boolean) => {
    if (is3d !== was3d) setTimeout(site.reload, 200);
  });
  return {};
}
