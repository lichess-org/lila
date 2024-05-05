import {
  init,
  attributesModule,
  eventListenersModule,
  classModule,
  propsModule,
  styleModule,
  h,
} from 'snabbdom';

import m, { MNode } from './mithrilFix';
import map from './map/mapMain';
import mapSide, { SideCtrl } from './map/mapSide';
import run from './run/runMain';
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

interface LearnServerOpts {
  data?: LearnProgress;
  i18n: I18nDict;
}

export function initModule({ data, i18n }: LearnServerOpts) {
  console.log('initializing learn module');

  const ctrl = {};
  const view = (ctrl: any) => h('div', `testing snabbdom ${ctrl}`);

  const snabbdomElement = document.getElementById('learn-app-snabbdom')!;
  snabbdomElement.innerHTML = '';
  let vnode = patch(snabbdomElement, view(ctrl));

  function redraw() {
    vnode = patch(vnode, view(ctrl));
  }

  redraw();

  // TODO: remove/refactor
  const element = document.getElementById('learn-app')!;
  const _storage = storage(data);

  const opts: LearnOpts = {
    i18n,
    storage: _storage,
    // Uninitialized because we need to call mapSide to initialize opts.side,
    // and we need opts to call mapSide.
    side: 'uninitialized' as any,
    stageId: null,
  };

  m.route.mode = 'hash';

  const trans = site.trans(opts.i18n);
  const side = mapSide(opts, trans);
  const sideCtrl = side.controller();

  opts.side = {
    ctrl: sideCtrl,
    view: function () {
      return side.view(sideCtrl);
    },
  };

  m.route(element, '/', {
    '/': map(opts, trans),
    '/:stage/:level': run(opts, trans),
    '/:stage': run(opts, trans),
  } as _mithril.MithrilRoutes<any>);

  return {};
}
