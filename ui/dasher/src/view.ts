import { h } from 'snabbdom'
import { VNode } from 'snabbdom/vnode'

import { DasherCtrl } from './dasher'
import links from './links'
import { view as langsView } from './langs'
import { spinner } from './util'

export default function(ctrl: DasherCtrl): VNode {
  let d = ctrl.data();
  let content: VNode | undefined;
  if (!d) content = h('div.initiating', spinner());
  else switch(ctrl.mode()) {
    case 'langs':
      content = langsView(ctrl.langs);
      break;
    default:
      content = links(ctrl, d);
  }
  return h('div#dasher_app.dropdown', content);
}
