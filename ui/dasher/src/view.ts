import { h, VNode } from 'snabbdom';
import { spinnerVdom as spinner } from 'common/spinner';

import DasherCtrl from './dasher';
import links from './links';
import { view as langsView } from './langs';
import { view as soundView } from './sound';
import { view as backgroundView } from './background';
import { view as boardView } from './board';
import { view as themeView } from './theme';
import { view as pieceView } from './piece';

export const loading = () => h('div#dasher_app.dropdown', h('div.initiating', spinner()));

export function loaded(ctrl: DasherCtrl): VNode {
  let content: VNode | undefined;
  switch (ctrl.mode()) {
    case 'langs':
      content = langsView(ctrl.langs);
      break;
    case 'sound':
      content = soundView(ctrl.sound);
      break;
    case 'background':
      content = backgroundView(ctrl.background);
      break;
    case 'board':
      content = boardView(ctrl.board);
      break;
    case 'theme':
      content = themeView(ctrl.theme);
      break;
    case 'piece':
      content = pieceView(ctrl.piece);
      break;
    default:
      content = links(ctrl);
  }
  return h('div#dasher_app.dropdown', content);
}
