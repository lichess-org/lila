import { h } from 'snabbdom'
import { VNode } from 'snabbdom/vnode'

import { DasherCtrl } from './dasher'
import links from './links'
import { view as langsView } from './langs'
import { view as soundView } from './sound'
import { view as backgroundView } from './background'
import { spinner } from './util'

export function loading(): VNode {
  return h('div#dasher_app.dropdown', spinner());
}

export function loaded(ctrl: DasherCtrl): VNode {
  let content: VNode | undefined;
  switch(ctrl.mode()) {
    case 'langs':
      content = langsView(ctrl.subs.langs);
      break;
    case 'sound':
      content = soundView(ctrl.subs.sound);
      break;
    case 'background':
      content = backgroundView(ctrl.subs.background);
      break;
    default:
      content = links(ctrl);
  }
  return h('div#dasher_app.dropdown', content);
}
