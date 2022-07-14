import ReplayCtrl from './ctrl';
import { h } from 'snabbdom';

export default function view(_ctrl: ReplayCtrl) {
  return h('div.replay', 'replay');
}
