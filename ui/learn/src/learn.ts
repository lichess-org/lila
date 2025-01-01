import {
  init,
  attributesModule,
  eventListenersModule,
  classModule,
  propsModule,
  styleModule,
} from 'snabbdom';
import { LearnCtrl } from './ctrl';
import { view } from './view';
import { Coords } from 'common/prefs';
import { pubsub } from 'common/pubsub';

import storage, { type Storage } from './storage';

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

export function initModule({ data, pref }: LearnServerOpts) {
  const _storage = storage(data);
  const opts: LearnOpts = {
    storage: _storage,
    stageId: null,
    levelId: null,
    pref: pref,
  };
  const ctrl = new LearnCtrl(opts, redraw);

  const element = document.getElementById('learn-app')!;
  element.innerHTML = '';
  const inner = document.createElement('div');
  element.appendChild(inner);
  let vnode = patch(inner, view(ctrl));

  function redraw() {
    vnode = patch(vnode, view(ctrl));
  }

  redraw();

  const was3d = document.head.querySelector(`link[data-css-key='common.board-3d']`) !== null;
  pubsub.on('board.change', (is3d: boolean) => {
    if (is3d !== was3d) setTimeout(site.reload, 200);
  });
  return {};
}
