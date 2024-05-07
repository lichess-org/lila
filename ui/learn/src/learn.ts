import {
  init,
  attributesModule,
  eventListenersModule,
  classModule,
  propsModule,
  styleModule,
} from 'snabbdom';
import { LearnCtrl } from './v2/ctrl';
import { view } from './v2/view';

import { MNode } from './mithrilFix';
import { SideCtrl } from './map/mapSide';
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
  side: {
    ctrl: SideCtrl;
    view(): MNode;
  };
  stageId: number | null;
  route?: string;
}

export interface SnabbdomLearnOpts {
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
  const snabbdomOpts: SnabbdomLearnOpts = {
    i18n,
    storage: _storage,
    stageId: null,
    levelId: null,
  };

  const ctrl = new LearnCtrl(snabbdomOpts, redraw);

  const element = document.getElementById('learn-app')!;
  element.innerHTML = '';
  const inner = document.createElement('div');
  element.appendChild(inner);
  let vnode = patch(inner, view(ctrl));

  function redraw() {
    vnode = patch(vnode, view(ctrl));
  }

  redraw();

  return {};
}
