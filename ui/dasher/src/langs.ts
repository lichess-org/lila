import { h } from 'snabbdom'
import { VNode } from 'snabbdom/vnode'

import { Redraw, Prop, prop } from './util'

export interface LangsCtrl {
}

export interface Lang {
  0: string,
  1: string
}

export function ctrl(redraw: Redraw): LangsCtrl {

  const data: Prop<Lang[] | undefined> = prop(undefined);

  return {
    data
  };
}

export function view(ctrl: LangsCtrl): VNode[] {

  return [
    h('div', 'langs')
  ];
}
