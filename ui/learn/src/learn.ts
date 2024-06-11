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

import storage, { Storage } from './storage';

const patch = init([classModule, attributesModule, propsModule, eventListenersModule, styleModule]);

export interface LearnProgress {
  _id?: string;
  stages: Record<string, StageProgress>;
}

export interface StageProgress {
  scores: number[];
}

export interface LearnOpts {
  i18n: I18nDict;
  storage: Storage;
  stageId: number | null;
  levelId: number | null;
  route?: string;
}

interface LearnServerOpts {
  data?: LearnProgress;
  i18n: I18nDict;
}

export function initModule({ data, i18n }: LearnServerOpts) {
  const _storage = storage(data);
  const opts: LearnOpts = {
    i18n,
    storage: _storage,
    stageId: null,
    levelId: null,
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
  site.pubsub.on('board.change', (is3d: boolean) => {
    if (is3d !== was3d) setTimeout(site.reload, 200);
  });
  return {};
}
